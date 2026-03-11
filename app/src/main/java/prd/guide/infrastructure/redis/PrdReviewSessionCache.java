package prd.guide.infrastructure.redis;

import prd.guide.common.model.AsyncTaskStatus;
import prd.guide.modules.prdreview.model.ReviewDetailLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.Serializable;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * PRD 评审会话 Redis 缓存服务
 * 管理 PRD 评审会话在 Redis 中的存储
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrdReviewSessionCache {

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    /**
     * 缓存键前缀
     */
    private static final String SESSION_KEY_PREFIX = "prd:review:session:";

    /**
     * PRD 内容哈希到会话ID的映射前缀（用于检测重复提交）
     */
    private static final String CONTENT_HASH_KEY_PREFIX = "prd:review:hash:";

    /**
     * 会话默认过期时间（24小时）
     */
    private static final Duration SESSION_TTL = Duration.ofHours(24);

    /**
     * 缓存的会话数据
     */
    @Data
    public static class CachedSession implements Serializable {
        private Long prdId;
        private String title;
        private String content;
        private ReviewDetailLevel detailLevel;
        private String knowledgeBaseIdsJson;
        private AsyncTaskStatus status;
        private String reviewResultJson;
        private String reviewError;

        public CachedSession() {
        }

        public CachedSession(Long prdId, String title, String content,
                            ReviewDetailLevel detailLevel, List<Long> knowledgeBaseIds,
                            AsyncTaskStatus status, ObjectMapper objectMapper) {
            this.prdId = prdId;
            this.title = title;
            this.content = content;
            this.detailLevel = detailLevel;
            this.status = status;
            try {
                this.knowledgeBaseIdsJson = objectMapper.writeValueAsString(knowledgeBaseIds);
            } catch (JacksonException e) {
                throw new RuntimeException("序列化知识库ID列表失败", e);
            }
        }

        public List<Long> getKnowledgeBaseIds(ObjectMapper objectMapper) {
            if (knowledgeBaseIdsJson == null || knowledgeBaseIdsJson.isBlank()) {
                return List.of();
            }
            try {
                return objectMapper.readValue(knowledgeBaseIdsJson, new TypeReference<>() {});
            } catch (JacksonException e) {
                throw new RuntimeException("反序列化知识库ID列表失败", e);
            }
        }
    }

    /**
     * 保存会话到缓存
     */
    public void saveSession(Long prdId, String title, String content,
                           ReviewDetailLevel detailLevel, List<Long> knowledgeBaseIds,
                           AsyncTaskStatus status) {
        String key = buildSessionKey(prdId);
        CachedSession cachedSession = new CachedSession(
            prdId, title, content, detailLevel, knowledgeBaseIds, status, objectMapper
        );

        redisService.set(key, cachedSession, SESSION_TTL);

        // 建立内容哈希映射（用于检测重复提交）
        if (content != null && !content.isBlank()) {
            String contentHash = generateContentHash(content);
            saveContentHashMapping(contentHash, prdId);
        }

        log.debug("PRD 评审会话已缓存: prdId={}, status={}", prdId, status);
    }

    /**
     * 获取缓存的会话
     */
    public Optional<CachedSession> getSession(Long prdId) {
        String key = buildSessionKey(prdId);
        CachedSession session = redisService.get(key);
        if (session != null) {
            log.debug("从缓存获取 PRD 评审会话: prdId={}", prdId);
            return Optional.of(session);
        }
        return Optional.empty();
    }

    /**
     * 更新会话状态
     */
    public void updateSessionStatus(Long prdId, AsyncTaskStatus status) {
        getSession(prdId).ifPresent(session -> {
            session.setStatus(status);
            String key = buildSessionKey(prdId);
            redisService.set(key, session, SESSION_TTL);

            // 如果会话已完成或失败，移除内容哈希映射
            if (status == AsyncTaskStatus.COMPLETED || status == AsyncTaskStatus.FAILED) {
                if (session.getContent() != null) {
                    String contentHash = generateContentHash(session.getContent());
                    removeContentHashMapping(contentHash, prdId);
                }
            }

            log.debug("更新 PRD 评审会话状态: prdId={}, status={}", prdId, status);
        });
    }

    /**
     * 更新评审结果
     */
    public void updateReviewResult(Long prdId, String reviewResultJson) {
        getSession(prdId).ifPresent(session -> {
            session.setReviewResultJson(reviewResultJson);
            session.setStatus(AsyncTaskStatus.COMPLETED);
            String key = buildSessionKey(prdId);
            redisService.set(key, session, SESSION_TTL);
            log.debug("更新 PRD 评审结果: prdId={}", prdId);
        });
    }

    /**
     * 更新评审错误
     */
    public void updateReviewError(Long prdId, String error) {
        getSession(prdId).ifPresent(session -> {
            session.setReviewError(error);
            session.setStatus(AsyncTaskStatus.FAILED);
            String key = buildSessionKey(prdId);
            redisService.set(key, session, SESSION_TTL);
            log.debug("更新 PRD 评审错误: prdId={}, error={}", prdId, error);
        });
    }

    /**
     * 删除会话缓存
     */
    public void deleteSession(Long prdId) {
        getSession(prdId).ifPresent(session -> {
            if (session.getContent() != null) {
                String contentHash = generateContentHash(session.getContent());
                removeContentHashMapping(contentHash, prdId);
            }
        });

        String key = buildSessionKey(prdId);
        redisService.delete(key);
        log.debug("删除 PRD 评审会话缓存: prdId={}", prdId);
    }

    /**
     * 根据内容哈希查找正在处理的会话ID
     * 用于检测重复提交
     */
    public Optional<Long> findProcessingSessionIdByContent(String content) {
        if (content == null || content.isBlank()) {
            return Optional.empty();
        }
        
        String contentHash = generateContentHash(content);
        String key = buildContentHashKey(contentHash);
        Long prdId = redisService.get(key);
        
        if (prdId != null) {
            // 验证会话是否仍然存在且正在处理
            Optional<CachedSession> sessionOpt = getSession(prdId);
            if (sessionOpt.isPresent() && isProcessingStatus(sessionOpt.get().getStatus())) {
                return Optional.of(prdId);
            } else {
                // 会话已不存在或已完成，清理映射
                redisService.delete(key);
            }
        }
        return Optional.empty();
    }

    /**
     * 刷新会话过期时间
     */
    public void refreshSessionTTL(Long prdId) {
        String key = buildSessionKey(prdId);
        redisService.expire(key, SESSION_TTL);
    }

    /**
     * 检查会话是否在缓存中
     */
    public boolean exists(Long prdId) {
        String key = buildSessionKey(prdId);
        return redisService.exists(key);
    }

    // ==================== 私有方法 ====================

    private String buildSessionKey(Long prdId) {
        return SESSION_KEY_PREFIX + prdId;
    }

    private String buildContentHashKey(String contentHash) {
        return CONTENT_HASH_KEY_PREFIX + contentHash;
    }

    private String generateContentHash(String content) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("生成内容哈希失败", e);
        }
    }

    private void saveContentHashMapping(String contentHash, Long prdId) {
        String key = buildContentHashKey(contentHash);
        redisService.set(key, prdId, SESSION_TTL);
    }

    private void removeContentHashMapping(String contentHash, Long prdId) {
        String key = buildContentHashKey(contentHash);
        Long currentPrdId = redisService.get(key);
        // 只有当前映射的是这个 prdId 时才删除
        if (prdId.equals(currentPrdId)) {
            redisService.delete(key);
        }
    }

    private boolean isProcessingStatus(AsyncTaskStatus status) {
        return status == AsyncTaskStatus.PENDING || status == AsyncTaskStatus.PROCESSING;
    }
}
