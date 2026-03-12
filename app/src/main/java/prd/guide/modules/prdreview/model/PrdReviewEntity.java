package prd.guide.modules.prdreview.model;

import jakarta.persistence.*;
import prd.guide.common.model.AsyncTaskStatus;
import java.time.LocalDateTime;

@Entity
@Table(name = "prd_reviews")
public class PrdReviewEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String originalContent;

    @Lob
    private String cleanedContent;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ReviewDetailLevel detailLevel;

    @Lob
    private String reviewResultJson;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AsyncTaskStatus reviewStatus = AsyncTaskStatus.PENDING;

    @Column(length = 500)
    private String reviewError;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
