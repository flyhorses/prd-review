package prd.guide.common.constant;

/**
 * 异步任务 Redis Stream 通用常量
 * 包含知识库向量化和 PRD 评审两个异步任务的配置
 */
public final class AsyncTaskStreamConstants {

    private AsyncTaskStreamConstants() {
        // 私有构造函数，防止实例化
    }

    // ========== 通用消息字段 ==========

    /**
     * 重试次数字段
     */
    public static final String FIELD_RETRY_COUNT = "retryCount";

    /**
     * 文档内容字段
     */
    public static final String FIELD_CONTENT = "content";

    // ========== 通用消费者配置 ==========

    /**
     * 最大重试次数
     */
    public static final int MAX_RETRY_COUNT = 3;

    /**
     * 每次拉取的消息批次大小
     */
    public static final int BATCH_SIZE = 10;

    /**
     * 消费者轮询间隔（毫秒）
     */
    public static final long POLL_INTERVAL_MS = 1000;

    /**
     * Stream 最大长度（自动裁剪旧消息，防止无限增长）
     */
    public static final int STREAM_MAX_LEN = 1000;

    // ========== 知识库向量化 Stream 配置 ==========

    /**
     * 知识库向量化 Stream Key
     */
    public static final String KB_VECTORIZE_STREAM_KEY = "knowledgebase:vectorize:stream";

    /**
     * 知识库向量化 Consumer Group 名称
     */
    public static final String KB_VECTORIZE_GROUP_NAME = "vectorize-group";

    /**
     * 知识库向量化 Consumer 名称前缀
     */
    public static final String KB_VECTORIZE_CONSUMER_PREFIX = "vectorize-consumer-";

    /**
     * 知识库ID字段
     */
    public static final String FIELD_KB_ID = "kbId";

    // ========== PRD 评审 Stream 配置 ==========

    /**
     * PRD 评审 Stream Key
     */
    public static final String PRD_REVIEW_STREAM_KEY = "prd:review:stream";

    /**
     * PRD 评审 Consumer Group 名称
     */
    public static final String PRD_REVIEW_GROUP_NAME = "prd-review-group";

    /**
     * PRD 评审 Consumer 名称前缀
     */
    public static final String PRD_REVIEW_CONSUMER_PREFIX = "prd-review-consumer-";

    /**
     * PRD 评审ID字段
     */
    public static final String FIELD_PRD_ID = "prdId";

    /**
     * PRD 标题字段
     */
    public static final String FIELD_TITLE = "title";

    /**
     * 评审粒度字段
     */
    public static final String FIELD_DETAIL_LEVEL = "detailLevel";

    /**
     * 知识库ID列表字段（JSON格式）
     */
    public static final String FIELD_KB_IDS = "kbIds";
}
