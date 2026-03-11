package prd.guide.modules.prdreview.model;

import java.util.List;

/**
 * PRD 评审结果。
 * <p>
 * 该结构同时作为 AI 结构化输出的目标 schema 使用，
 * 可直接用于 {@code BeanOutputConverter<PrdReviewResponse>}。
 */
public record PrdReviewResponse(
    /**
     * 总体评价概要（用通俗语言描述 PRD 的整体质量与主要问题）。
     */
    String summary,

    /**
     * 需求清晰度维度评审结果。
     */
    DimensionEvaluation clarity,

    /**
     * 范围与边界维度评审结果。
     */
    DimensionEvaluation scope,

    /**
     * 用户场景 / 流程维度评审结果。
     */
    DimensionEvaluation userFlows,

    /**
     * 技术风险与复杂度维度评审结果。
     */
    DimensionEvaluation techRisk,

    /**
     * 指标与验收标准维度评审结果。
     */
    DimensionEvaluation metrics,

    /**
     * 工作量 / 排期相关评审结果（偏建议性质，而非精确工时）。
     */
    DimensionEvaluation estimation,

    /**
     * 综合改进建议清单（跨维度的整体优化建议）。
     */
    List<String> overallSuggestions
) {

    /**
     * 单个评审维度结果。
     * <p>
     * 约定 score 取值范围为 0–100；issues / suggestions 按重要程度排序。
     */
    public record DimensionEvaluation(
        /**
         * 该维度评分，0–100 分，数值越高表示质量越好。
         */
        Integer score,

        /**
         * 在该维度下发现的问题点列表。
         */
        List<String> issues,

        /**
         * 针对上述问题的具体改进建议列表。
         */
        List<String> suggestions
    ) {
    }
}


