package prd.guide.modules.prdreview.model;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * PRD 评审请求
 */
public record PrdReviewRequest(
    String title,
    @NotBlank(message = "PRD内容不能为空")
    String content,
    ReviewDetailLevel detailLevel,
    /**
     * 是否启用知识库提示（RAG）。
     * <p>
     * 当为 true 时，服务端会尝试从指定的知识库中检索与当前 PRD 相关的规范 / 最佳实践片段，
     * 并在评审提示词中作为额外上下文提供给大模型。
     */
    Boolean enableKnowledgeBaseHints,
    /**
     * 可选的知识库 ID 列表。
     * <p>
     * - 为空或 null：当前实现将不启用知识库检索。
     * - 非空：在这些知识库范围内进行向量检索。
     */
    List<Long> knowledgeBaseIds
) {
}

