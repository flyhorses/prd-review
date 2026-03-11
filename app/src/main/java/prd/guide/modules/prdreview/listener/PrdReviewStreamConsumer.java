package prd.guide.modules.prdreview.listener;

import prd.guide.common.async.AbstractStreamConsumer;
import prd.guide.common.constant.AsyncTaskStreamConstants;
import prd.guide.common.model.AsyncTaskStatus;
import prd.guide.infrastructure.redis.RedisService;
import prd.guide.modules.knowledgebase.service.KnowledgeBaseQueryService;
import prd.guide.modules.prdreview.model.PrdPreprocessResult;
import prd.guide.modules.prdreview.model.PrdReviewEntity;
import prd.guide.modules.prdreview.model.PrdReviewResponse;
import prd.guide.modules.prdreview.model.ReviewDetailLevel;
import prd.guide.modules.prdreview.service.PrdPreprocessService;
import prd.guide.modules.prdreview.service.PrdReviewAiService;
import prd.guide.modules.prdreview.service.PrdReviewPersistenceService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.stream.StreamMessageId;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PRD 评审 Stream 消费者
 * 负责从 Redis Stream 消费消息并执行 AI 评审
 */
@Slf4j
@Component
public class PrdReviewStreamConsumer extends AbstractStreamConsumer<PrdReviewStreamConsumer.PrdReviewPayload> {

    private final PrdPreprocessService preprocessService;
    private final PrdReviewAiService aiService;
    private final KnowledgeBaseQueryService knowledgeBaseQueryService;
    private final PrdReviewPersistenceService persistenceService;

    record PrdReviewPayload(
        Long prdId,
        String title,
        String content,
        ReviewDetailLevel detailLevel,
        List<Long> knowledgeBaseIds
    ) {}

    public PrdReviewStreamConsumer(
        RedisService redisService,
        PrdPreprocessService preprocessService,
        PrdReviewAiService aiService,
        KnowledgeBaseQueryService knowledgeBaseQueryService,
        PrdReviewPersistenceService persistenceService
    ) {
        super(redisService);
        this.preprocessService = preprocessService;
        this.aiService = aiService;
        this.knowledgeBaseQueryService = knowledgeBaseQueryService;
        this.persistenceService = persistenceService;
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
    protected String groupName() {
        return AsyncTaskStreamConstants.PRD_REVIEW_GROUP_NAME;
    }

    @Override
    protected String consumerPrefix() {
        return AsyncTaskStreamConstants.PRD_REVIEW_CONSUMER_PREFIX;
    }

    @Override
    protected String threadName() {
        return "prd-review-consumer";
    }

    @Override
    protected PrdReviewPayload parsePayload(StreamMessageId messageId, Map<String, String> data) {
        String prdIdStr = data.get(AsyncTaskStreamConstants.FIELD_PRD_ID);
        String content = data.get(AsyncTaskStreamConstants.FIELD_CONTENT);
        if (prdIdStr == null || content == null) {
            log.warn("消息格式错误，跳过: messageId={}", messageId);
            return null;
        }

        String title = data.get(AsyncTaskStreamConstants.FIELD_TITLE);
        String detailLevelStr = data.get(AsyncTaskStreamConstants.FIELD_DETAIL_LEVEL);
        ReviewDetailLevel detailLevel = ReviewDetailLevel.BASIC;
        if (detailLevelStr != null && !detailLevelStr.isBlank()) {
            try {
                detailLevel = ReviewDetailLevel.valueOf(detailLevelStr);
            } catch (IllegalArgumentException e) {
                log.warn("无效的评审粒度: {}, 使用默认值 BASIC", detailLevelStr);
            }
        }

        List<Long> kbIds = parseKbIds(data.get(AsyncTaskStreamConstants.FIELD_KB_IDS));

        return new PrdReviewPayload(
            Long.parseLong(prdIdStr),
            title,
            content,
            detailLevel,
            kbIds
        );
    }

    @Override
    protected String payloadIdentifier(PrdReviewPayload payload) {
        return "prdId=" + payload.prdId();
    }

    @Override
    protected void markProcessing(PrdReviewPayload payload) {
        persistenceService.updateReviewStatus(payload.prdId(), AsyncTaskStatus.PROCESSING, null);
    }

    @Override
    protected void processBusiness(PrdReviewPayload payload) {
        Long prdId = payload.prdId();
        PrdReviewEntity entity = persistenceService.findEntityById(prdId);
        if (entity == null) {
            log.warn("PRD 已被删除，跳过评审任务: prdId={}", prdId);
            return;
        }

        PrdPreprocessResult preprocessResult = preprocessService.preprocess(payload.content());
        String cleanedContent = preprocessResult.cleanedContent();
        if (cleanedContent == null || cleanedContent.isBlank()) {
            throw new IllegalStateException("PRD 内容为空或仅包含空白字符");
        }

        String knowledgeBaseContext = buildKnowledgeBaseContext(payload, preprocessResult);

        PrdReviewResponse response = aiService.review(
            payload.title(),
            preprocessResult,
            payload.detailLevel(),
            knowledgeBaseContext
        );

        persistenceService.updateReviewResult(prdId, cleanedContent, payload.detailLevel(), response);
    }

    @Override
    protected void markCompleted(PrdReviewPayload payload) {
        persistenceService.updateReviewStatus(payload.prdId(), AsyncTaskStatus.COMPLETED, null);
    }

    @Override
    protected void markFailed(PrdReviewPayload payload, String error) {
        persistenceService.updateReviewStatus(payload.prdId(), AsyncTaskStatus.FAILED, error);
    }

    @Override
    protected void retryMessage(PrdReviewPayload payload, int retryCount) {
        Long prdId = payload.prdId();
        try {
            Map<String, String> message = new java.util.HashMap<>();
            message.put(AsyncTaskStreamConstants.FIELD_PRD_ID, prdId.toString());
            message.put(AsyncTaskStreamConstants.FIELD_TITLE, payload.title() != null ? payload.title() : "");
            message.put(AsyncTaskStreamConstants.FIELD_CONTENT, payload.content());
            message.put(AsyncTaskStreamConstants.FIELD_DETAIL_LEVEL, 
                payload.detailLevel() != null ? payload.detailLevel().name() : ReviewDetailLevel.BASIC.name());
            message.put(AsyncTaskStreamConstants.FIELD_RETRY_COUNT, String.valueOf(retryCount));

            if (payload.knowledgeBaseIds() != null && !payload.knowledgeBaseIds().isEmpty()) {
                message.put(AsyncTaskStreamConstants.FIELD_KB_IDS,
                    payload.knowledgeBaseIds().stream()
                        .map(String::valueOf)
                        .reduce((a, b) -> a + "," + b)
                        .orElse(""));
            }

            redisService().streamAdd(
                AsyncTaskStreamConstants.PRD_REVIEW_STREAM_KEY,
                message,
                AsyncTaskStreamConstants.STREAM_MAX_LEN
            );
            log.info("PRD 评审任务已重新入队: prdId={}, retryCount={}", prdId, retryCount);

        } catch (Exception e) {
            log.error("重试入队失败: prdId={}, error={}", prdId, e.getMessage(), e);
            persistenceService.updateReviewStatus(prdId, AsyncTaskStatus.FAILED, truncateError("重试入队失败: " + e.getMessage()));
        }
    }

    /**
     * 解析知识库 ID 列表
     */
    private List<Long> parseKbIds(String kbIdsStr) {
        if (kbIdsStr == null || kbIdsStr.isBlank()) {
            return List.of();
        }
        List<Long> result = new ArrayList<>();
        for (String idStr : kbIdsStr.split(",")) {
            try {
                result.add(Long.parseLong(idStr.trim()));
            } catch (NumberFormatException e) {
                log.warn("无效的知识库ID: {}", idStr);
            }
        }
        return result;
    }

    /**
     * 构建知识库上下文
     */
    private String buildKnowledgeBaseContext(PrdReviewPayload payload, PrdPreprocessResult preprocessResult) {
        if (payload.knowledgeBaseIds() == null || payload.knowledgeBaseIds().isEmpty()) {
            return "";
        }

        String cleanedContent = preprocessResult.cleanedContent();
        if (cleanedContent == null || cleanedContent.isBlank()) {
            return "";
        }

        StringBuilder questionBuilder = new StringBuilder();
        questionBuilder.append("请基于以下 PRD 内容，检索与产品需求文档编写规范、设计最佳实践、")
            .append("技术风险评估、验收标准等相关的内部知识片段，用于辅助评审：");
        if (payload.title() != null && !payload.title().isBlank()) {
            questionBuilder.append("标题：").append(payload.title()).append("。");
        }
        String limitedContent = cleanedContent.length() > 2000
            ? cleanedContent.substring(0, 2000)
            : cleanedContent;
        questionBuilder.append("PRD 摘要：").append(limitedContent);

        return knowledgeBaseQueryService.buildContextForQuestion(
            payload.knowledgeBaseIds(),
            questionBuilder.toString()
        );
    }
}
