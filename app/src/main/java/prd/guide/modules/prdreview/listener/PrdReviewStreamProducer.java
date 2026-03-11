package prd.guide.modules.prdreview.listener;

import prd.guide.common.async.AbstractStreamProducer;
import prd.guide.common.constant.AsyncTaskStreamConstants;
import prd.guide.common.model.AsyncTaskStatus;
import prd.guide.infrastructure.redis.RedisService;
import prd.guide.modules.prdreview.model.ReviewDetailLevel;
import prd.guide.modules.prdreview.repository.PrdReviewRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PRD 评审任务生产者
 * 负责发送评审任务到 Redis Stream
 */
@Slf4j
@Component
public class PrdReviewStreamProducer extends AbstractStreamProducer<PrdReviewStreamProducer.PrdReviewTaskPayload> {

    private final PrdReviewRepository prdReviewRepository;

    record PrdReviewTaskPayload(
        Long prdId,
        String title,
        String content,
        ReviewDetailLevel detailLevel,
        List<Long> knowledgeBaseIds
    ) {}

    public PrdReviewStreamProducer(RedisService redisService, PrdReviewRepository prdReviewRepository) {
        super(redisService);
        this.prdReviewRepository = prdReviewRepository;
    }

    /**
     * 发送评审任务到 Redis Stream
     *
     * @param prdId            PRD 记录ID
     * @param title            PRD 标题
     * @param content          PRD 内容
     * @param detailLevel      评审粒度
     * @param knowledgeBaseIds 知识库ID列表（可选）
     */
    public void sendReviewTask(Long prdId, String title, String content,
                               ReviewDetailLevel detailLevel, List<Long> knowledgeBaseIds) {
        sendTask(new PrdReviewTaskPayload(prdId, title, content, detailLevel, knowledgeBaseIds));
    }

    @Override
    protected String taskDisplayName() {
        return "PRD评审";
    }

    @Override
    protected String streamKey() {
        return AsyncTaskStreamConstants.PRD_REVIEW_STREAM_KEY;
    }

    @Override
    protected Map<String, String> buildMessage(PrdReviewTaskPayload payload) {
        Map<String, String> message = new HashMap<>();
        message.put(AsyncTaskStreamConstants.FIELD_PRD_ID, payload.prdId().toString());
        message.put(AsyncTaskStreamConstants.FIELD_TITLE, payload.title() != null ? payload.title() : "");
        message.put(AsyncTaskStreamConstants.FIELD_CONTENT, payload.content());
        message.put(AsyncTaskStreamConstants.FIELD_DETAIL_LEVEL, 
            payload.detailLevel() != null ? payload.detailLevel().name() : ReviewDetailLevel.BASIC.name());
        message.put(AsyncTaskStreamConstants.FIELD_RETRY_COUNT, "0");
        
        if (payload.knowledgeBaseIds() != null && !payload.knowledgeBaseIds().isEmpty()) {
            message.put(AsyncTaskStreamConstants.FIELD_KB_IDS, 
                payload.knowledgeBaseIds().stream()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + "," + b)
                    .orElse(""));
        }
        
        return message;
    }

    @Override
    protected String payloadIdentifier(PrdReviewTaskPayload payload) {
        return "prdId=" + payload.prdId();
    }

    @Override
    protected void onSendFailed(PrdReviewTaskPayload payload, String error) {
        updateReviewStatus(payload.prdId(), AsyncTaskStatus.FAILED, truncateError(error));
    }

    /**
     * 更新评审状态
     */
    private void updateReviewStatus(Long prdId, AsyncTaskStatus status, String error) {
        prdReviewRepository.findById(prdId).ifPresent(prd -> {
            prd.setReviewStatus(status);
            if (error != null) {
                prd.setReviewError(error.length() > 500 ? error.substring(0, 500) : error);
            }
            prdReviewRepository.save(prd);
        });
    }
}
