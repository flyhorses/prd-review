package prd.guide.infrastructure.mapper;

import prd.guide.common.model.AsyncTaskStatus;
import prd.guide.modules.prdreview.model.PrdReviewEntity;
import prd.guide.modules.prdreview.model.ReviewDetailLevel;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * PRD 评审相关的对象映射器
 * 使用MapStruct自动生成转换代码
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PrdReviewMapper {

    /**
     * PrdReviewEntity 转换为 PrdReviewListItemDTO
     */
    default PrdReviewListItemDTO toListItemDTO(PrdReviewEntity entity) {
        if (entity == null) {
            return null;
        }
        
        Integer overallScore = calculateOverallScore(entity);
        
        return new PrdReviewListItemDTO(
            entity.getId(),
            entity.getTitle(),
            entity.getDetailLevel() != null ? entity.getDetailLevel().name() : ReviewDetailLevel.BASIC.name(),
            entity.getReviewStatus() != null ? entity.getReviewStatus().name() : AsyncTaskStatus.PENDING.name(),
            entity.getReviewError(),
            overallScore,
            entity.getCreatedAt()
        );
    }

    /**
     * 批量转换 PRD 评审列表
     */
    List<PrdReviewListItemDTO> toListItemDTOList(List<PrdReviewEntity> entities);

    /**
     * 计算总体评分
     * 从 reviewResultJson 中提取各维度评分并计算平均值
     */
    private Integer calculateOverallScore(PrdReviewEntity entity) {
        if (entity.getReviewResultJson() == null || entity.getReviewResultJson().isBlank()) {
            return null;
        }
        
        try {
            int totalScore = 0;
            int count = 0;
            
            if (entity.getReviewResultJson().contains("\"clarity\"")) {
                totalScore += extractDimensionScore(entity.getReviewResultJson(), "clarity");
                count++;
            }
            if (entity.getReviewResultJson().contains("\"scope\"")) {
                totalScore += extractDimensionScore(entity.getReviewResultJson(), "scope");
                count++;
            }
            if (entity.getReviewResultJson().contains("\"userFlows\"")) {
                totalScore += extractDimensionScore(entity.getReviewResultJson(), "userFlows");
                count++;
            }
            if (entity.getReviewResultJson().contains("\"techRisk\"")) {
                totalScore += extractDimensionScore(entity.getReviewResultJson(), "techRisk");
                count++;
            }
            if (entity.getReviewResultJson().contains("\"metrics\"")) {
                totalScore += extractDimensionScore(entity.getReviewResultJson(), "metrics");
                count++;
            }
            if (entity.getReviewResultJson().contains("\"estimation\"")) {
                totalScore += extractDimensionScore(entity.getReviewResultJson(), "estimation");
                count++;
            }
            
            return count > 0 ? totalScore / count : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从 JSON 中提取指定维度的评分
     */
    private int extractDimensionScore(String json, String dimension) {
        try {
            String pattern = "\"" + dimension + "\":\\s*\\{[^}]*\"score\":\\s*(\\d+)";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(json);
            if (m.find()) {
                return Integer.parseInt(m.group(1));
            }
        } catch (Exception ignored) {
        }
        return 0;
    }

    /**
     * PRD 评审列表项 DTO
     */
    record PrdReviewListItemDTO(
        Long id,
        String title,
        String detailLevel,
        String reviewStatus,
        String reviewError,
        Integer overallScore,
        java.time.LocalDateTime createdAt
    ) {}
}
