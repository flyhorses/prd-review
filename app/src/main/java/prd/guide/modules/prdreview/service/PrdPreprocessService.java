package prd.guide.modules.prdreview.service;

import prd.guide.modules.prdreview.model.PrdPreprocessResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * PRD 文本预处理服务：
 * - 对 Markdown 文本做基础清洗（去掉多余空行、统一换行符等）
 * - 做一次基于标题的简单结构切分，识别常见 PRD 章节
 */
@Service
public class PrdPreprocessService {

    /**
     * 常见 PRD 章节关键词映射到规范化 key
     */
    private static final Map<String, String> SECTION_KEYWORDS = new LinkedHashMap<>();

    /**
     * Markdown 标题行的基础匹配（# / ## / ### 开头）
     */
    private static final Pattern HEADING_PATTERN = Pattern.compile("^\\s{0,3}#{1,6}\\s+(.+)$");

    static {
        // 背景 / 目标 / 范围 / 用户 / 流程 / 技术方案 / 风险 / 验收 / 指标等
        SECTION_KEYWORDS.put("background", "background");
        SECTION_KEYWORDS.put("背景", "background");

        SECTION_KEYWORDS.put("goal", "goal");
        SECTION_KEYWORDS.put("目标", "goal");
        SECTION_KEYWORDS.put("目的", "goal");

        SECTION_KEYWORDS.put("scope", "scope");
        SECTION_KEYWORDS.put("范围", "scope");
        SECTION_KEYWORDS.put("边界", "scope");

        SECTION_KEYWORDS.put("user story", "userStories");
        SECTION_KEYWORDS.put("用户故事", "userStories");
        SECTION_KEYWORDS.put("用户需求", "userStories");

        SECTION_KEYWORDS.put("flow", "flows");
        SECTION_KEYWORDS.put("流程", "flows");
        SECTION_KEYWORDS.put("用例", "flows");

        SECTION_KEYWORDS.put("tech", "techSolution");
        SECTION_KEYWORDS.put("技术方案", "techSolution");
        SECTION_KEYWORDS.put("技术设计", "techSolution");
        SECTION_KEYWORDS.put("架构设计", "techSolution");

        SECTION_KEYWORDS.put("risk", "risks");
        SECTION_KEYWORDS.put("风险", "risks");

        SECTION_KEYWORDS.put("metric", "metrics");
        SECTION_KEYWORDS.put("指标", "metrics");
        SECTION_KEYWORDS.put("验收标准", "metrics");
        SECTION_KEYWORDS.put("验收", "metrics");
    }

    /**
     * 对原始 PRD Markdown 文本进行基础预处理与简单结构分析
     *
     * @param content 原始 Markdown 文本
     * @return 预处理结果（清洗后的全文 + 识别出的章节）
     */
    public PrdPreprocessResult preprocess(String content) {
        if (content == null || content.isBlank()) {
            return new PrdPreprocessResult("", Map.of());
        }

        String normalized = normalizeLineEndings(content);
        List<String> cleanedLines = removeRedundantBlankLines(normalized);
        String cleanedContent = String.join("\n", cleanedLines).trim();

        Map<String, PrdPreprocessResult.Section> sections = analyzeStructure(cleanedLines);
        return new PrdPreprocessResult(cleanedContent, sections);
    }

    private String normalizeLineEndings(String content) {
        return content.replace("\r\n", "\n").replace("\r", "\n");
    }

    private List<String> removeRedundantBlankLines(String content) {
        String[] lines = content.split("\n", -1);
        List<String> result = new ArrayList<>(lines.length);
        boolean lastBlank = false;
        for (String line : lines) {
            String trimmed = line.stripTrailing();
            boolean currentBlank = trimmed.isBlank();
            if (currentBlank && lastBlank) {
                continue;
            }
            result.add(trimmed);
            lastBlank = currentBlank;
        }
        return result;
    }

    /**
     * 基于 Markdown 标题行做一次简单章节识别
     */
    private Map<String, PrdPreprocessResult.Section> analyzeStructure(List<String> lines) {
        Map<String, PrdPreprocessResult.Section> result = new LinkedHashMap<>();

        String currentKey = null;
        String currentTitle = null;
        int currentStart = -1;
        List<String> currentLines = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher matcher = HEADING_PATTERN.matcher(line);
            if (matcher.matches()) {
                // 先把上一个 section 收尾
                if (currentKey != null) {
                    int endLine = i - 1;
                    if (!currentLines.isEmpty()) {
                        result.putIfAbsent(
                            currentKey,
                            new PrdPreprocessResult.Section(
                                currentKey,
                                currentTitle,
                                currentStart,
                                endLine,
                                List.copyOf(currentLines)
                            )
                        );
                    }
                    currentLines = new ArrayList<>();
                }

                String headingText = matcher.group(1).trim();
                String matchedKey = matchSectionKey(headingText);
                if (matchedKey != null) {
                    currentKey = matchedKey;
                    currentTitle = headingText;
                    currentStart = i;
                    continue;
                } else {
                    // 不是关心的章节标题，视为普通行
                    if (currentKey != null) {
                        currentLines.add(line);
                    }
                }
            } else {
                if (currentKey != null) {
                    currentLines.add(line);
                }
            }
        }

        // 收尾最后一个 section
        if (currentKey != null && !currentLines.isEmpty()) {
            result.putIfAbsent(
                currentKey,
                new PrdPreprocessResult.Section(
                    currentKey,
                    currentTitle,
                    currentStart,
                    lines.size() - 1,
                    List.copyOf(currentLines)
                )
            );
        }

        return result;
    }

    private String matchSectionKey(String headingText) {
        String lower = headingText.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> entry : SECTION_KEYWORDS.entrySet()) {
            String keyword = entry.getKey();
            if (lower.contains(keyword) || headingText.contains(keyword)) {
                return entry.getValue();
            }
        }
        return null;
    }
}

