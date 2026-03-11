package prd.guide.modules.prdreview.model;

import java.util.List;
import java.util.Map;

/**
 * PRD 预处理结果：
 * - cleanedContent: 经过基础清洗后的 Markdown 文本（用于直接喂给大模型）
 * - sections: 依据常见 PRD 章节标题做的简单结构切分结果
 */
public record PrdPreprocessResult(
    String cleanedContent,
    Map<String, Section> sections
) {

    public record Section(
        String key,
        String title,
        int startLine,
        int endLine,
        List<String> lines
    ) {
    }
}

