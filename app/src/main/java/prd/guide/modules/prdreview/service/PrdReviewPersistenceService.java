package prd.guide.modules.prdreview.service;

import prd.guide.common.exception.BusinessException;
import prd.guide.common.exception.ErrorCode;
import prd.guide.common.model.AsyncTaskStatus;
import prd.guide.modules.prdreview.model.PrdPreprocessResult;
import prd.guide.modules.prdreview.model.PrdReviewEntity;
import prd.guide.modules.prdreview.model.PrdReviewRequest;
import prd.guide.modules.prdreview.model.PrdReviewResponse;
import prd.guide.modules.prdreview.model.ReviewDetailLevel;
import prd.guide.modules.prdreview.repository.PrdReviewRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * PRD 评审结果持久化服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrdReviewPersistenceService {

    private final PrdReviewRepository reviewRepository;
    private final ObjectMapper objectMapper;

    /**
     * 创建一个 PENDING 状态的 PRD 记录（用于异步处理）
     */
    @Transactional(rollbackFor = Exception.class)
    public PrdReviewEntity createPendingRecord(PrdReviewRequest request) {
        PrdReviewEntity entity = new PrdReviewEntity();
        entity.setTitle(request.title());
        entity.setOriginalContent(request.content());
        entity.setDetailLevel(request.detailLevel() != null ? request.detailLevel() : ReviewDetailLevel.BASIC);
        entity.setReviewStatus(AsyncTaskStatus.PENDING);

        PrdReviewEntity saved = reviewRepository.save(entity);
        log.info("PRD 评审记录已创建（待处理）: id={}", saved.getId());
        return saved;
    }

    /**
     * 保存一次 PRD 评审结果
     */
    @Transactional(rollbackFor = Exception.class)
    public PrdReviewEntity saveReview(PrdReviewRequest request,
                                      PrdPreprocessResult preprocessResult,
                                      PrdReviewResponse response) {
        try {
            PrdReviewEntity entity = new PrdReviewEntity();
            entity.setTitle(request.title());
            entity.setOriginalContent(request.content());
            entity.setCleanedContent(preprocessResult.cleanedContent());
            entity.setDetailLevel(request.detailLevel());
            entity.setReviewResultJson(objectMapper.writeValueAsString(response));
            entity.setReviewStatus(AsyncTaskStatus.COMPLETED);

            PrdReviewEntity saved = reviewRepository.save(entity);
            log.info("PRD 评审结果已保存: id={}", saved.getId());
            return saved;
        } catch (JacksonException e) {
            log.error("序列化 PRD 评审结果失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存 PRD 评审结果失败");
        }
    }

    /**
     * 根据ID获取历史评审结果
     */
    @Transactional(readOnly = true)
    public PrdReviewResponse findReviewResponseById(Long id) {
        PrdReviewEntity entity = reviewRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "PRD 评审记录不存在"));

        if (entity.getReviewResultJson() == null || entity.getReviewResultJson().isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "PRD 评审结果尚未生成");
        }

        try {
            return objectMapper.readValue(entity.getReviewResultJson(), PrdReviewResponse.class);
        } catch (JacksonException e) {
            log.error("反序列化 PRD 评审结果失败, id={}", id, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "读取 PRD 评审结果失败");
        }
    }

    /**
     * 根据ID获取评审实体（包含状态信息）
     */
    @Transactional(readOnly = true)
    public PrdReviewEntity findEntityById(Long id) {
        return reviewRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "PRD 评审记录不存在"));
    }

    /**
     * 保存评审实体
     */
    @Transactional(rollbackFor = Exception.class)
    public PrdReviewEntity save(PrdReviewEntity entity) {
        return reviewRepository.save(entity);
    }

    /**
     * 获取所有评审记录（按创建时间倒序）
     */
    @Transactional(readOnly = true)
    public List<PrdReviewEntity> findAllByOrderByCreatedAtDesc() {
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 删除评审记录
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "PRD 评审记录不存在");
        }
        reviewRepository.deleteById(id);
        log.info("PRD 评审记录已删除: id={}", id);
    }

    /**
     * 更新评审状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateReviewStatus(Long prdId, AsyncTaskStatus status, String error) {
        reviewRepository.findById(prdId).ifPresent(entity -> {
            entity.setReviewStatus(status);
            entity.setReviewError(error);
            reviewRepository.save(entity);
            log.debug("评审状态已更新: prdId={}, status={}", prdId, status);
        });
    }

    /**
     * 更新评审结果
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateReviewResult(Long prdId, String cleanedContent, 
                                   ReviewDetailLevel detailLevel, PrdReviewResponse response) {
        reviewRepository.findById(prdId).ifPresent(entity -> {
            entity.setCleanedContent(cleanedContent);
            entity.setDetailLevel(detailLevel);
            try {
                entity.setReviewResultJson(objectMapper.writeValueAsString(response));
                entity.setReviewStatus(AsyncTaskStatus.COMPLETED);
                reviewRepository.save(entity);
                log.info("PRD 评审结果已保存: prdId={}", prdId);
            } catch (JacksonException e) {
                log.error("序列化 PRD 评审结果失败: prdId={}", prdId, e);
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存评审结果失败");
            }
        });
    }
}

