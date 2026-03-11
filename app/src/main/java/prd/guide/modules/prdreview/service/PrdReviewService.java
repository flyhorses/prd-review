package prd.guide.modules.prdreview.service;

import prd.guide.common.exception.BusinessException;
import prd.guide.common.exception.ErrorCode;
import prd.guide.common.model.AsyncTaskStatus;
import prd.guide.infrastructure.export.PdfExportService;
import prd.guide.infrastructure.mapper.PrdReviewMapper;
import prd.guide.infrastructure.mapper.PrdReviewMapper.PrdReviewListItemDTO;
import prd.guide.modules.knowledgebase.service.KnowledgeBaseQueryService;
import prd.guide.modules.prdreview.listener.PrdReviewStreamProducer;
import prd.guide.modules.prdreview.model.PrdPreprocessResult;
import prd.guide.modules.prdreview.model.PrdReviewEntity;
import prd.guide.modules.prdreview.model.PrdReviewRequest;
import prd.guide.modules.prdreview.model.PrdReviewResponse;
import prd.guide.modules.prdreview.model.ReviewDetailLevel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * PRD 评审核心服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PrdReviewService {

    private final PrdPreprocessService preprocessService;
    private final PrdReviewAiService aiService;
    private final PrdReviewPersistenceService persistenceService;
    private final KnowledgeBaseQueryService knowledgeBaseQueryService;
    private final PrdReviewStreamProducer streamProducer;
    private final PdfExportService pdfExportService;
    private final PrdReviewMapper prdReviewMapper;

    /**
     * 对 PRD 进行评审并返回结果（同步模式）
     */
    public PrdReviewResponse review(PrdReviewRequest request) {
        PrdPreprocessResult preprocessResult = preprocessService.preprocess(request.content());
        String cleaned = preprocessResult.cleanedContent();
        if (cleaned == null || cleaned.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "PRD内容不能为空或仅包含空白字符");
        }

        ReviewDetailLevel level = request.detailLevel() != null
            ? request.detailLevel()
            : ReviewDetailLevel.BASIC;

        String knowledgeBaseContext = buildKnowledgeBaseContextIfEnabled(request, preprocessResult);

        PrdReviewResponse response = aiService.review(
            request.title(),
            preprocessResult,
            level,
            knowledgeBaseContext
        );
        persistenceService.saveReview(request, preprocessResult, response);
        return response;
    }

    /**
     * 提交 PRD 评审任务（异步模式）
     * 立即返回 PRD 记录 ID，评审在后台异步执行
     *
     * @param request PRD 评审请求
     * @return PRD 记录 ID
     */
    public Long submitReviewAsync(PrdReviewRequest request) {
        if (request.content() == null || request.content().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "PRD内容不能为空");
        }

        PrdReviewEntity entity = persistenceService.createPendingRecord(request);
        Long prdId = entity.getId();

        streamProducer.sendReviewTask(
            prdId,
            request.title(),
            request.content(),
            request.detailLevel(),
            request.knowledgeBaseIds()
        );

        log.info("PRD 评审任务已提交: prdId={}", prdId);
        return prdId;
    }

    /**
     * 获取 PRD 评审状态
     *
     * @param id PRD 记录 ID
     * @return 评审状态
     */
    public AsyncTaskStatus getReviewStatus(Long id) {
        PrdReviewEntity entity = persistenceService.findEntityById(id);
        return entity.getReviewStatus();
    }

    /**
     * 根据 ID 获取评审结果（包含状态信息）
     */
    public PrdReviewResponse getReview(Long id) {
        return persistenceService.findReviewResponseById(id);
    }

    /**
     * 重新评审（手动重试）
     */
    public void reReview(Long id) {
        PrdReviewEntity entity = persistenceService.findEntityById(id);
        
        if (entity.getReviewStatus() == AsyncTaskStatus.PROCESSING) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "PRD 正在评审中，请稍后再试");
        }

        entity.setReviewStatus(AsyncTaskStatus.PENDING);
        entity.setReviewError(null);
        persistenceService.save(entity);
        
        streamProducer.sendReviewTask(
            id,
            entity.getTitle(),
            entity.getOriginalContent(),
            entity.getDetailLevel(),
            null
        );

        log.info("PRD 重新评审任务已提交: prdId={}", id);
    }

    /**
     * 导出 PRD 评审报告为 PDF
     *
     * @param id PRD 记录 ID
     * @return PDF 字节数组
     */
    public byte[] exportReviewPdf(Long id) {
        PrdReviewEntity entity = persistenceService.findEntityById(id);
        
        if (entity.getReviewStatus() != AsyncTaskStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "PRD 评审尚未完成，无法导出");
        }
        
        PrdReviewResponse response = persistenceService.findReviewResponseById(id);
        return pdfExportService.exportPrdReviewReport(entity, response);
    }

    /**
     * 获取评审记录列表
     */
    public List<PrdReviewListItemDTO> getReviewList() {
        List<PrdReviewEntity> entities = persistenceService.findAllByOrderByCreatedAtDesc();
        return prdReviewMapper.toListItemDTOList(entities);
    }

    /**
     * 获取评审详情（包含实体信息）
     */
    public Map<String, Object> getReviewDetail(Long id) {
        PrdReviewEntity entity = persistenceService.findEntityById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("id", entity.getId());
        result.put("title", entity.getTitle());
        result.put("detailLevel", entity.getDetailLevel() != null ? entity.getDetailLevel().name() : "BASIC");
        result.put("status", entity.getReviewStatus() != null ? entity.getReviewStatus().name() : "PENDING");
        result.put("createdAt", entity.getCreatedAt());
        result.put("reviewError", entity.getReviewError());
        
        if (entity.getReviewResultJson() != null && !entity.getReviewResultJson().isBlank()) {
            result.put("hasResult", true);
        } else {
            result.put("hasResult", false);
        }
        
        return result;
    }

    /**
     * 删除评审记录
     */
    public void deleteReview(Long id) {
        persistenceService.deleteById(id);
    }

    /**
     * 根据请求参数决定是否启用知识库提示，并构建用于 PRD 评审的上下文文本。
     */
    private String buildKnowledgeBaseContextIfEnabled(PrdReviewRequest request, PrdPreprocessResult preprocessResult) {
        if (!Boolean.TRUE.equals(request.enableKnowledgeBaseHints())) {
            return "";
        }
        if (request.knowledgeBaseIds() == null || request.knowledgeBaseIds().isEmpty()) {
            return "";
        }

        String cleanedContent = preprocessResult.cleanedContent();
        if (cleanedContent == null || cleanedContent.isBlank()) {
            return "";
        }

        StringBuilder questionBuilder = new StringBuilder();
        questionBuilder.append("请基于以下 PRD 内容，检索与产品需求文档编写规范、设计最佳实践、")
            .append("技术风险评估、验收标准等相关的内部知识片段，用于辅助评审：");
        if (request.title() != null && !request.title().isBlank()) {
            questionBuilder.append("标题：").append(request.title()).append("。");
        }
        String limitedContent = cleanedContent.length() > 2000
            ? cleanedContent.substring(0, 2000)
            : cleanedContent;
        questionBuilder.append("PRD 摘要：").append(limitedContent);

        return knowledgeBaseQueryService.buildContextForQuestion(
            request.knowledgeBaseIds(),
            questionBuilder.toString()
        );
    }
}
