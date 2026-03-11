package prd.guide.modules.prdreview.service;

import prd.guide.common.ai.StructuredOutputInvoker;
import prd.guide.common.exception.BusinessException;
import prd.guide.common.exception.ErrorCode;
import prd.guide.modules.prdreview.model.PrdPreprocessResult;
import prd.guide.modules.prdreview.model.PrdReviewResponse;
import prd.guide.modules.prdreview.model.ReviewDetailLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于大模型的 PRD 评审引擎。
 * <p>
 * 负责将预处理后的 PRD 文本和简单结构信息，转换为结构化评审结果 {@link PrdReviewResponse}。
 */
@Slf4j
@Service
public class PrdReviewAiService {

    private final ChatClient chatClient;
    private final StructuredOutputInvoker structuredOutputInvoker;
    private final PromptTemplate systemPromptTemplate;
    private final PromptTemplate userPromptTemplate;
    private final BeanOutputConverter<PrdReviewResponse> outputConverter;

    public PrdReviewAiService(
        ChatClient.Builder chatClientBuilder,
        StructuredOutputInvoker structuredOutputInvoker,
        @Value("classpath:prompts/prd-review-system.st") Resource systemPromptResource,
        @Value("classpath:prompts/prd-review-user.st") Resource userPromptResource
    ) throws IOException {
        this.chatClient = chatClientBuilder.build();
        this.structuredOutputInvoker = structuredOutputInvoker;
        this.systemPromptTemplate = new PromptTemplate(systemPromptResource.getContentAsString(StandardCharsets.UTF_8));
        this.userPromptTemplate = new PromptTemplate(userPromptResource.getContentAsString(StandardCharsets.UTF_8));
        this.outputConverter = new BeanOutputConverter<>(PrdReviewResponse.class);
    }

    /**
     * 调用大模型，对 PRD 进行结构化评审。
     *
     * @param title            PRD 标题（可为空）
     * @param preprocessResult 预处理结果（清洗后的全文 + 章节切分）
     * @param detailLevel      评审细致程度
     * @return 结构化评审结果
     */
    public PrdReviewResponse review(
        String title,
        PrdPreprocessResult preprocessResult,
        ReviewDetailLevel detailLevel
    ) {
        return review(title, preprocessResult, detailLevel, null);
    }

    /**
     * 调用大模型，对 PRD 进行结构化评审，并可选地附加知识库上下文。
     *
     * @param title                PRD 标题（可为空）
     * @param preprocessResult     预处理结果（清洗后的全文 + 章节切分）
     * @param detailLevel          评审细致程度
     * @param knowledgeBaseContext 来自知识库的补充上下文（可为空或空字符串）
     * @return 结构化评审结果
     */
    public PrdReviewResponse review(
        String title,
        PrdPreprocessResult preprocessResult,
        ReviewDetailLevel detailLevel,
        String knowledgeBaseContext
    ) {
        String cleanedContent = preprocessResult.cleanedContent();
        ReviewDetailLevel safeLevel = detailLevel != null ? detailLevel : ReviewDetailLevel.BASIC;

        log.info("开始 PRD 评审，title={}, length={}, detailLevel={}",
            title, cleanedContent != null ? cleanedContent.length() : 0, safeLevel);

        try {
            String systemPrompt = systemPromptTemplate.render();

            Map<String, Object> variables = new HashMap<>();
            variables.put("title", title != null ? title : "");
            variables.put("content", cleanedContent != null ? cleanedContent : "");
            variables.put("detailLevel", safeLevel.name());
            variables.put("sectionsSummary", buildSectionsSummary(preprocessResult));
            variables.put("knowledgeBaseContext", knowledgeBaseContext != null ? knowledgeBaseContext : "");

            String userPrompt = userPromptTemplate.render(variables);

            // 将输出格式说明追加到 system prompt，便于 BeanOutputConverter 正确解析
            String systemPromptWithFormat = systemPrompt + "\n\n" + outputConverter.getFormat();

            PrdReviewResponse response = structuredOutputInvoker.invoke(
                chatClient,
                systemPromptWithFormat,
                userPrompt,
                outputConverter,
                ErrorCode.AI_SERVICE_ERROR,
                "PRD 评审失败：",
                "PRD 评审",
                log
            );

            log.info("PRD 评审完成");
            return response;
        } catch (BusinessException e) {
            // 统一使用业务异常向上抛出，交给全局异常处理
            throw e;
        } catch (Exception e) {
            log.error("PRD 评审 AI 调用失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR, "PRD 评审失败：" + e.getMessage());
        }
    }

    /**
     * 将预处理阶段识别出的章节结构压缩为适合提示词的摘要字符串。
     */
    private String buildSectionsSummary(PrdPreprocessResult preprocessResult) {
        if (preprocessResult.sections() == null || preprocessResult.sections().isEmpty()) {
            return "未能识别出明显的章节结构，请基于完整文本进行评审。";
        }

        StringBuilder sb = new StringBuilder();
        preprocessResult.sections().forEach((key, section) -> {
            sb.append("[").append(key).append("] ").append(section.title()).append("\n");
            String joined = String.join("\n", section.lines());
            if (joined.length() > 800) {
                sb.append(joined, 0, 800).append("...\n\n");
            } else {
                sb.append(joined).append("\n\n");
            }
        });
        return sb.toString();
    }
}

