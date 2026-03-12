package prd.guide.modules.prdreview.service;

import prd.guide.common.exception.BusinessException;
import prd.guide.common.exception.ErrorCode;
import prd.guide.common.model.AsyncTaskStatus;
import prd.guide.common.security.SecurityUtils;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PrdReviewPersistenceService {

    private final PrdReviewRepository reviewRepository;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public PrdReviewEntity createPendingRecord(PrdReviewRequest request) {
        PrdReviewEntity entity = new PrdReviewEntity();
        entity.setUserId(getCurrentUserId());
        entity.setTitle(request.title());
        entity.setOriginalContent(request.content());
        entity.setDetailLevel(request.detailLevel() != null ? request.detailLevel() : ReviewDetailLevel.BASIC);
        entity.setReviewStatus(AsyncTaskStatus.PENDING);

        PrdReviewEntity saved = reviewRepository.save(entity);
        log.info("PRD 评审记录已创建（待处理）: id={}, userId={}", saved.getId(), saved.getUserId());
        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public PrdReviewEntity saveReview(PrdReviewRequest request,
                                      PrdPreprocessResult preprocessResult,
                                      PrdReviewResponse response) {
        try {
            PrdReviewEntity entity = new PrdReviewEntity();
            entity.setUserId(getCurrentUserId());
            entity.setTitle(request.title());
            entity.setOriginalContent(request.content());
            entity.setCleanedContent(preprocessResult.cleanedContent());
            entity.setDetailLevel(request.detailLevel());
            entity.setReviewResultJson(objectMapper.writeValueAsString(response));
            entity.setReviewStatus(AsyncTaskStatus.COMPLETED);

            PrdReviewEntity saved = reviewRepository.save(entity);
            log.info("PRD 评审结果已保存: id={}, userId={}", saved.getId(), saved.getUserId());
            return saved;
        } catch (JacksonException e) {
            log.error("序列化 PRD 评审结果失败", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存 PRD 评审结果失败");
        }
    }

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

    @Transactional(readOnly = true)
    public PrdReviewEntity findEntityById(Long id) {
        return reviewRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "PRD 评审记录不存在"));
    }

    @Transactional(rollbackFor = Exception.class)
    public PrdReviewEntity save(PrdReviewEntity entity) {
        return reviewRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<PrdReviewEntity> findAllByOrderByCreatedAtDesc() {
        Long userId = getCurrentUserId();
        if (userId != null) {
            return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }
        return reviewRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "PRD 评审记录不存在");
        }
        reviewRepository.deleteById(id);
        log.info("PRD 评审记录已删除: id={}", id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateReviewStatus(Long prdId, AsyncTaskStatus status, String error) {
        reviewRepository.findById(prdId).ifPresent(entity -> {
            entity.setReviewStatus(status);
            entity.setReviewError(error);
            reviewRepository.save(entity);
            log.debug("评审状态已更新: prdId={}, status={}", prdId, status);
        });
    }

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

    private Long getCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            log.warn("无法获取当前用户ID，使用默认值");
        }
        return userId;
    }
}
