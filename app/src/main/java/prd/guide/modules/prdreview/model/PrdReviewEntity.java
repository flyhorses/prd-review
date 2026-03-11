package prd.guide.modules.prdreview.model;

import prd.guide.common.model.AsyncTaskStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * PRD 评审持久化实体
 */
@Entity
@Table(name = "prd_reviews", indexes = {
    @Index(name = "idx_prd_review_created_at", columnList = "createdAt"),
    @Index(name = "idx_prd_review_status", columnList = "reviewStatus")
})
public class PrdReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * PRD 标题（可选）
     */
    @Column(length = 255)
    private String title;

    /**
     * 原始 PRD Markdown 内容
     */
    @Lob
    @Column(nullable = false)
    private String originalContent;

    /**
     * 预处理后的清洗文本
     */
    @Lob
    private String cleanedContent;

    /**
     * 评审粒度
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReviewDetailLevel detailLevel;

    /**
     * 评审结果 JSON（对应 {@link PrdReviewResponse}）
     */
    @Lob
    private String reviewResultJson;

    /**
     * 评审状态：PENDING / PROCESSING / COMPLETED / FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AsyncTaskStatus reviewStatus = AsyncTaskStatus.PENDING;

    /**
     * 评审错误信息（失败时记录）
     */
    @Column(length = 500)
    private String reviewError;

    /**
     * 创建时间
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getCleanedContent() {
        return cleanedContent;
    }

    public void setCleanedContent(String cleanedContent) {
        this.cleanedContent = cleanedContent;
    }

    public ReviewDetailLevel getDetailLevel() {
        return detailLevel;
    }

    public void setDetailLevel(ReviewDetailLevel detailLevel) {
        this.detailLevel = detailLevel;
    }

    public String getReviewResultJson() {
        return reviewResultJson;
    }

    public void setReviewResultJson(String reviewResultJson) {
        this.reviewResultJson = reviewResultJson;
    }

    public AsyncTaskStatus getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(AsyncTaskStatus reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getReviewError() {
        return reviewError;
    }

    public void setReviewError(String reviewError) {
        this.reviewError = reviewError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

