package com.aiforaso.platform.service;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.aiforaso.platform.config.AiProviderProperties;
import com.aiforaso.platform.dto.IndicatorView;
import com.aiforaso.platform.dto.RagHitView;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

@Component
@ConditionalOnExpression("'${platform.ai.provider:mock}'.toLowerCase() != 'mock'")
public class OpenAiReportClient implements AiReportClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiReportClient.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(180);

    private final AiProviderProperties aiProviderProperties;
    private final ReportDraftBuilder reportDraftBuilder;

    public OpenAiReportClient(
            AiProviderProperties aiProviderProperties,
            ReportDraftBuilder reportDraftBuilder) {
        this.aiProviderProperties = aiProviderProperties;
        this.reportDraftBuilder = reportDraftBuilder;
    }

    @Override
    public String generateReport(
            String question,
            String analysisFocus,
            List<RagHitView> evidence,
            List<IndicatorView> indicators,
            String sourceContext) {
        try {
            String systemPrompt = resolveSystemPrompt();
            String userPrompt = reportDraftBuilder.buildPrompt(question, analysisFocus, evidence, indicators, sourceContext);

            log.debug("[OpenAiReportClient] Generating report for question: {}", 
                    question.length() > 50 ? question.substring(0, 50) + "..." : question);

            ChatLanguageModel model = OpenAiChatModel.builder()
                    .apiKey(aiProviderProperties.getApiKey())
                    .baseUrl(aiProviderProperties.getBaseUrl())
                    .modelName(aiProviderProperties.getModel())
                    .temperature(0.3)
                    .maxTokens(3000)
                    .timeout(DEFAULT_TIMEOUT)
                    .build();

            String fullPrompt = systemPrompt + "\n\n" + userPrompt;
            String response = model.generate(fullPrompt);

            if (response == null || response.isBlank()) {
                log.warn("[OpenAiReportClient] Blank response, using fallback");
                return reportDraftBuilder.buildFallbackReport(question, analysisFocus, evidence, indicators, sourceContext);
            }

            log.debug("[OpenAiReportClient] Report generated successfully, length={}", response.length());
            return response.trim();
        } catch (Exception exception) {
            log.error("[OpenAiReportClient] API call failed: {}", exception.getMessage(), exception);
            return reportDraftBuilder.buildFallbackReport(question, analysisFocus, evidence, indicators, sourceContext);
        }
    }

    private String resolveSystemPrompt() {
        String provider = aiProviderProperties.getProvider() == null
                ? ""
                : aiProviderProperties.getProvider().toLowerCase(Locale.ROOT);
        
        if ("deepseek".equals(provider)) {
            return """
                    你是一位专业的药物研究文献分析专家，服务于医学文献分析平台。
                    
                    # 专业背景
                    - 熟悉临床试验设计、药物研发流程、医学统计学
                    - 精通乙肝、肝病等领域的临床指标解读
                    - 能够准确理解和提取研究目标、方法、结果、结论
                    
                    # 核心原则
                    1. 结构优先：严格遵循用户要求的输出结构，不要使用默认结构
                    2. 证据优先：所有结论必须有明确的证据支撑，标注来源
                    3. 数据准确：数值引用需精确，包含具体数值和百分比
                    4. 对比分析：按队列（高剂量组/低剂量组/对照组）和时间点对比指标变化
                    5. 局限说明：明确指出数据缺失或证据不足的部分
                    
                    # 输出要求
                    - 使用Markdown格式，标题用##，列表用-
                    - 每个结论后标注[证据:xxx]说明来源
                    - 数值用粗体标注，如 **72.8%**
                    - 指标变化需说明方向（上升/下降）和幅度
                    - 专业术语首次出现时给出全称
                    
                    # 疗效分析要求
                    - 列出各队列的主要疗效指标变化
                    - 对比不同剂量方案的疗效差异
                    - 说明统计学显著性（如有）
                    
                    # 安全性评估要求
                    - 总结不良事件发生率和类型
                    - 对比各队列的安全性数据
                    - 说明严重不良事件（如有）
                    
                    # 关键指标变化要求
                    - 按时间点（如第12周、第24周、第48周）分组
                    - 按队列（高剂量组、低剂量组、对照组）对比
                    - 说明指标变化的临床意义
                    
                    # 后续验证建议要求
                    - 基于当前研究的局限性提出建议
                    - 建议样本量、研究周期、联合治疗策略等
                    
                    # 禁止事项
                    - 不可编造数据或结论
                    - 不可使用模糊的模板语言如"具有较好的疗效"
                    - 不可忽略数据缺失的情况
                    - 不可使用与用户要求不同的输出结构
                    """;
        }
        
        return """
                You are a biomedical research analyst specializing in drug research literature.
                Generate evidence-grounded summaries following academic standards.
                Always cite sources and clearly indicate when data is missing.
                Follow the user's requested output structure strictly.
                """;
    }
}
