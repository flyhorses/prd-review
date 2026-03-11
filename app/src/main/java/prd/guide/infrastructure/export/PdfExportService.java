package prd.guide.infrastructure.export;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.font.PdfFontFactory.EmbeddingStrategy;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import prd.guide.common.exception.BusinessException;
import prd.guide.common.exception.ErrorCode;
import prd.guide.modules.prdreview.model.PrdReviewEntity;
import prd.guide.modules.prdreview.model.PrdReviewResponse;
import prd.guide.modules.prdreview.model.ReviewDetailLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

/**
 * PDF导出服务
 * PDF Export Service for PRD review reports
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfExportService {
    
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb SECTION_COLOR = new DeviceRgb(52, 73, 94);
    
    private final ObjectMapper objectMapper;
    
    /**
     * 创建支持中文的字体
     */
    private PdfFont createChineseFont() {
        try {
            var fontStream = getClass().getClassLoader().getResourceAsStream("fonts/ZhuqueFangsong-Regular.ttf");
            if (fontStream != null) {
                byte[] fontBytes = fontStream.readAllBytes();
                fontStream.close();
                log.debug("使用项目内嵌字体: fonts/ZhuqueFangsong-Regular.ttf");
                return PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H, EmbeddingStrategy.FORCE_EMBEDDED);
            }
            
            log.error("未找到字体文件: fonts/ZhuqueFangsong-Regular.ttf");
            throw new BusinessException(ErrorCode.EXPORT_PDF_FAILED, "字体文件缺失，请联系管理员");
            
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("创建中文字体失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.EXPORT_PDF_FAILED, "创建字体失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理文本中可能导致字体问题的字符
     */
    private String sanitizeText(String text) {
        if (text == null) return "";
        return text.replaceAll("[\\p{So}\\p{Cs}]", "").trim();
    }
    
    /**
     * 导出 PRD 评审报告为 PDF
     */
    public byte[] exportPrdReviewReport(PrdReviewEntity entity, PrdReviewResponse response) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);
        
        PdfFont font = createChineseFont();
        document.setFont(font);
        
        Paragraph title = new Paragraph("PRD 评审报告")
            .setFontSize(24)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(HEADER_COLOR);
        document.add(title);
        
        document.add(new Paragraph("\n"));
        document.add(createSectionTitle("基本信息"));
        document.add(new Paragraph("PRD 标题: " + (entity.getTitle() != null ? entity.getTitle() : "无标题")));
        document.add(new Paragraph("评审粒度: " + getDetailLevelText(entity.getDetailLevel())));
        document.add(new Paragraph("创建时间: " + 
            (entity.getCreatedAt() != null ? DATE_FORMAT.format(entity.getCreatedAt()) : "未知")));
        
        int overallScore = calculateOverallScore(response);
        document.add(new Paragraph("\n"));
        document.add(createSectionTitle("综合评分"));
        Paragraph scoreP = new Paragraph("总分: " + overallScore + " / 100")
            .setFontSize(18)
            .setBold()
            .setFontColor(getScoreColor(overallScore));
        document.add(scoreP);
        
        if (response.clarity() != null || response.scope() != null || 
            response.userFlows() != null || response.techRisk() != null ||
            response.metrics() != null || response.estimation() != null) {
            document.add(new Paragraph("\n"));
            document.add(createSectionTitle("各维度评分"));
            
            Table scoreTable = new Table(UnitValue.createPercentArray(new float[]{2, 1}))
                .useAllAvailableWidth();
            addDimensionScore(scoreTable, "需求清晰度", response.clarity());
            addDimensionScore(scoreTable, "范围与边界", response.scope());
            addDimensionScore(scoreTable, "用户场景/流程", response.userFlows());
            addDimensionScore(scoreTable, "技术风险", response.techRisk());
            addDimensionScore(scoreTable, "指标与验收标准", response.metrics());
            addDimensionScore(scoreTable, "工作量评估", response.estimation());
            document.add(scoreTable);
        }
        
        if (response.summary() != null) {
            document.add(new Paragraph("\n"));
            document.add(createSectionTitle("总体评价"));
            document.add(new Paragraph(sanitizeText(response.summary())));
        }
        
        addDimensionSection(document, "需求清晰度", response.clarity());
        addDimensionSection(document, "范围与边界", response.scope());
        addDimensionSection(document, "用户场景/流程", response.userFlows());
        addDimensionSection(document, "技术风险", response.techRisk());
        addDimensionSection(document, "指标与验收标准", response.metrics());
        addDimensionSection(document, "工作量评估", response.estimation());
        
        if (response.overallSuggestions() != null && !response.overallSuggestions().isEmpty()) {
            document.add(new Paragraph("\n"));
            document.add(createSectionTitle("综合改进建议"));
            for (String suggestion : response.overallSuggestions()) {
                document.add(new Paragraph("• " + sanitizeText(suggestion)));
            }
        }
        
        document.close();
        return baos.toByteArray();
    }
    
    /**
     * 添加维度评分行
     */
    private void addDimensionScore(Table table, String dimensionName, PrdReviewResponse.DimensionEvaluation evaluation) {
        if (evaluation == null || evaluation.score() == null) {
            return;
        }
        table.addCell(new com.itextpdf.layout.element.Cell().add(new Paragraph(dimensionName)));
        table.addCell(new com.itextpdf.layout.element.Cell().add(
            new Paragraph(evaluation.score() + " / 100")
                .setFontColor(getScoreColor(evaluation.score()))));
    }
    
    /**
     * 添加维度详细章节
     */
    private void addDimensionSection(Document document, String dimensionName, 
                                     PrdReviewResponse.DimensionEvaluation evaluation) {
        if (evaluation == null) {
            return;
        }
        
        boolean hasContent = (evaluation.issues() != null && !evaluation.issues().isEmpty()) ||
                            (evaluation.suggestions() != null && !evaluation.suggestions().isEmpty());
        
        if (!hasContent) {
            return;
        }
        
        document.add(new Paragraph("\n"));
        document.add(createSectionTitle(dimensionName + " (评分: " + 
            (evaluation.score() != null ? evaluation.score() : "-") + ")"));
        
        if (evaluation.issues() != null && !evaluation.issues().isEmpty()) {
            document.add(new Paragraph("问题点:").setBold());
            for (String issue : evaluation.issues()) {
                document.add(new Paragraph("  • " + sanitizeText(issue)));
            }
        }
        
        if (evaluation.suggestions() != null && !evaluation.suggestions().isEmpty()) {
            document.add(new Paragraph("改进建议:").setBold());
            for (String suggestion : evaluation.suggestions()) {
                document.add(new Paragraph("  • " + sanitizeText(suggestion)));
            }
        }
    }
    
    /**
     * 计算总体评分
     */
    private int calculateOverallScore(PrdReviewResponse response) {
        int totalScore = 0;
        int count = 0;
        
        if (response.clarity() != null && response.clarity().score() != null) {
            totalScore += response.clarity().score();
            count++;
        }
        if (response.scope() != null && response.scope().score() != null) {
            totalScore += response.scope().score();
            count++;
        }
        if (response.userFlows() != null && response.userFlows().score() != null) {
            totalScore += response.userFlows().score();
            count++;
        }
        if (response.techRisk() != null && response.techRisk().score() != null) {
            totalScore += response.techRisk().score();
            count++;
        }
        if (response.metrics() != null && response.metrics().score() != null) {
            totalScore += response.metrics().score();
            count++;
        }
        if (response.estimation() != null && response.estimation().score() != null) {
            totalScore += response.estimation().score();
            count++;
        }
        
        return count > 0 ? totalScore / count : 0;
    }
    
    private Paragraph createSectionTitle(String title) {
        return new Paragraph(title)
            .setFontSize(14)
            .setBold()
            .setFontColor(SECTION_COLOR)
            .setMarginTop(10);
    }
    
    private DeviceRgb getScoreColor(int score) {
        if (score >= 80) return new DeviceRgb(39, 174, 96);
        if (score >= 60) return new DeviceRgb(241, 196, 15);
        return new DeviceRgb(231, 76, 60);
    }
    
    private String getDetailLevelText(ReviewDetailLevel level) {
        if (level == null) {
            return "基础评审";
        }
        return switch (level) {
            case BASIC -> "基础评审";
            case DETAILED -> "详细评审";
            case COMPREHENSIVE -> "全面评审";
        };
    }
}
