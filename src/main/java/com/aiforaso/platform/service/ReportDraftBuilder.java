package com.aiforaso.platform.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.aiforaso.platform.config.AiProviderProperties;
import com.aiforaso.platform.dto.IndicatorView;
import com.aiforaso.platform.dto.RagHitView;

@Component
public class ReportDraftBuilder {

    private final AiProviderProperties aiProviderProperties;

    public ReportDraftBuilder(AiProviderProperties aiProviderProperties) {
        this.aiProviderProperties = aiProviderProperties;
    }

    public String buildPrompt(
            String question,
            String analysisFocus,
            List<RagHitView> evidence,
            List<IndicatorView> indicators,
            String sourceContext) {
        
        String evidenceBlock = buildEvidenceBlock(evidence);
        String indicatorBlock = buildIndicatorBlock(indicators);

        return """
                # 任务
                基于提供的证据和指标数据，生成一份结构化的药物研究文献分析报告。
                
                # 核心要求
                1. 必须基于提供的证据和数据，不可编造或推测
                2. 使用中文回答（除非用户明确要求英文）
                3. 数据缺失时明确说明，不要用模板语言填充
                4. 每个结论需标注证据来源
                
                # 输出结构
                %s
                
                # 分析重点
                %s
                
                # 用户问题
                %s
                
                # 文献上下文
                %s
                
                # RAG检索证据
                %s
                
                # 抽取指标
                %s
                """.formatted(
                resolveSectionGuide(question),
                StringUtils.hasText(analysisFocus) ? analysisFocus : "综合文献分析",
                question,
                StringUtils.hasText(sourceContext) ? sourceContext : "无",
                evidenceBlock,
                indicatorBlock);
    }

    private String buildEvidenceBlock(List<RagHitView> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return "无检索证据";
        }
        
        return evidence.stream()
                .limit(8)
                .map(hit -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("【").append(safeValue(hit.title(), "未命名文献")).append("】");
                    if (StringUtils.hasText(hit.evidenceLocator())) {
                        sb.append(" 定位:").append(hit.evidenceLocator());
                    }
                    sb.append(" 相似度:").append(String.format("%.2f", hit.score()));
                    sb.append("\n").append("内容: ").append(trimTo(hit.snippet(), 150));
                    return sb.toString();
                })
                .collect(Collectors.joining("\n\n"));
    }

    private String buildIndicatorBlock(List<IndicatorView> indicators) {
        if (indicators == null || indicators.isEmpty()) {
            return "无抽取指标";
        }
        
        return indicators.stream()
                .limit(10)
                .map(indicator -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("指标: ").append(safeValue(indicator.indicatorName(), "未知"));
                    sb.append(" | 类别: ").append(safeValue(indicator.category(), "未知"));
                    sb.append(" | 时间: ").append(safeValue(indicator.timeWindow(), "未指定"));
                    sb.append(" | 队列: ").append(safeValue(indicator.cohort(), "总体"));
                    if (indicator.observedValue() != null) {
                        sb.append(" | 值: ").append(indicator.observedValue());
                    }
                    if (StringUtils.hasText(indicator.evidenceLocator())) {
                        sb.append(" | 证据: ").append(indicator.evidenceLocator());
                    }
                    return sb.toString();
                })
                .collect(Collectors.joining("\n"));
    }

    public String buildFallbackReport(
            String question,
            String analysisFocus,
            List<RagHitView> evidence,
            List<IndicatorView> indicators,
            String sourceContext) {
        boolean chinese = containsChinese(question) || containsChinese(analysisFocus) || containsChinese(sourceContext);
        return chinese
                ? buildChineseFallback(question, analysisFocus, evidence, indicators, sourceContext)
                : buildEnglishFallback(question, analysisFocus, evidence, indicators, sourceContext);
    }

    private String buildChineseFallback(
            String question,
            String analysisFocus,
            List<RagHitView> evidence,
            List<IndicatorView> indicators,
            String sourceContext) {
        List<String> evidenceSnippets = evidence.stream()
                .map(hit -> {
                    String locator = StringUtils.hasText(hit.evidenceLocator()) ? "（定位：" + hit.evidenceLocator() + "）" : "";
                    return "- " + safeValue(hit.title(), "未命名文献") + locator + "：" + trimTo(hit.snippet(), 120);
                })
                .filter(item -> !item.endsWith("："))
                .limit(5)
                .toList();

        List<String> indicatorLines = indicators.stream()
                .map(indicator -> {
                    StringBuilder builder = new StringBuilder("- ").append(safeValue(indicator.indicatorName(), "未命名指标"));
                    if (StringUtils.hasText(indicator.timeWindow())) {
                        builder.append("，时间点=").append(indicator.timeWindow());
                    }
                    if (StringUtils.hasText(indicator.cohort())) {
                        builder.append("，队列=").append(indicator.cohort());
                    }
                    if (indicator.observedValue() != null) {
                        builder.append("，观测值=").append(indicator.observedValue());
                    }
                    if (StringUtils.hasText(indicator.evidenceLocator())) {
                        builder.append("，证据定位=").append(indicator.evidenceLocator());
                    }
                    return builder.toString();
                })
                .limit(8)
                .toList();

        StringBuilder report = new StringBuilder();

        if (isObjectiveMethodResultMeaningQuestion(question)) {
            report.append("## 研究目标\n");
            report.append(extractContextLine(sourceContext, "研究目标：", "当前未从检索结果中直接命中研究目标，以下内容来自文献原文解析。")).append("\n\n");

            report.append("## 研究方法\n");
            report.append(extractContextLine(sourceContext, "研究方法：", "当前未从检索结果中直接命中研究方法，以下内容来自文献原文解析。")).append("\n\n");

            report.append("## 主要结果\n");
            if (!StringUtils.hasText(extractContextLine(sourceContext, "主要结果：", "")) && indicatorLines.isEmpty()) {
                report.append("当前未命中足够的结构化结果，建议先检查文献分块、向量化和指标抽取状态。").append("\n");
            } else {
                String resultLine = extractContextLine(sourceContext, "主要结果：", "");
                if (StringUtils.hasText(resultLine)) {
                    report.append(resultLine).append("\n");
                }
                indicatorLines.forEach(line -> report.append(line).append("\n"));
            }
            report.append("\n");

            report.append("## 研究意义\n");
            report.append(extractContextLine(sourceContext, "研究意义：", "当前结论主要基于文献原文解析，仍建议结合原文段落做最终确认。")).append("\n\n");

            report.append("## 证据与局限\n");
            if (!evidenceSnippets.isEmpty()) {
                evidenceSnippets.forEach(line -> report.append(line).append("\n"));
            } else {
                report.append("- 当前没有命中稳定的 RAG 证据片段，本次总结主要基于当前文献原文解析结果。\n");
            }
            report.append("- 分析侧重点：").append(StringUtils.hasText(analysisFocus) ? analysisFocus : "文献综合总结").append("\n");
            return report.toString().trim();
        }

        report.append("## 研究结论\n");
        report.append(extractContextLine(sourceContext, "研究意义：", "当前结论主要基于文献原文解析结果。")).append("\n\n");

        report.append("## 证据综合\n");
        if (!evidenceSnippets.isEmpty()) {
            evidenceSnippets.forEach(line -> report.append(line).append("\n"));
        } else if (StringUtils.hasText(sourceContext)) {
            report.append("- 当前没有命中稳定的 RAG 证据片段，以下结论主要依据文献原文解析得到。\n");
            report.append(trimTo(sourceContext, 600)).append("\n");
        } else {
            report.append("- 当前没有可用证据。\n");
        }
        report.append("\n");

        report.append("## 指标解读\n");
        if (!indicatorLines.isEmpty()) {
            indicatorLines.forEach(line -> report.append(line).append("\n"));
        } else {
            report.append("- 当前没有抽取到稳定的结构化指标，建议先检查分块和指标抽取流程。\n");
        }
        report.append("\n");

        report.append("## 风险与局限\n");
        if (evidence.isEmpty()) {
            report.append("- 当前没有命中稳定的 RAG 证据片段，本次总结更多依赖文献原文解析。\n");
        }
        if (indicators.isEmpty()) {
            report.append("- 当前没有抽取到足够的结构化指标，定量解读仍有局限。\n");
        }
        if (!StringUtils.hasText(sourceContext)) {
            report.append("- 当前也没有足够的原文上下文，建议重新检查导入文献和解析流程。\n");
        }
        if (!report.toString().contains("风险与局限\n-")) {
            report.append("- 当前结果可用于快速阅读，但正式使用前仍建议回到原文核对。\n");
        }
        report.append("\n");

        report.append("## 下一步建议\n");
        report.append("- 先核对关键指标对应的原文段落和时间点。\n");
        report.append("- 如需答辩展示，优先展示研究目标、方法、主要结果和研究意义四部分。\n");

        return report.toString().trim();
    }

    private String buildEnglishFallback(
            String question,
            String analysisFocus,
            List<RagHitView> evidence,
            List<IndicatorView> indicators,
            String sourceContext) {
        String evidenceSummary = evidence.stream()
                .map(hit -> safeValue(hit.title(), "Untitled")
                        + "@"
                        + safeValue(hit.evidenceLocator(), "summary")
                        + ": "
                        + trimTo(hit.snippet(), 120))
                .limit(5)
                .collect(Collectors.joining("; "));
        String indicatorSummary = indicators.stream()
                .map(indicator -> safeValue(indicator.indicatorName(), "indicator")
                        + "@"
                        + safeValue(indicator.timeWindow(), "unspecified"))
                .limit(8)
                .collect(Collectors.joining(", "));

        return """
                ## Research conclusion
                %s

                ## Evidence synthesis
                %s

                ## Indicator interpretation
                %s

                ## Risks and limitations
                %s

                ## Suggested next validation step
                Validate the structured indicators against source paragraphs before final interpretation.
                """.formatted(
                StringUtils.hasText(sourceContext) ? trimTo(sourceContext, 300) : "The current summary is limited by missing source context.",
                StringUtils.hasText(evidenceSummary) ? evidenceSummary : "No stable retrieval evidence is currently available.",
                StringUtils.hasText(indicatorSummary) ? indicatorSummary : "No stable structured indicators are currently available.",
                StringUtils.hasText(analysisFocus) ? "Current focus: " + analysisFocus : "Current output is suitable only for quick review.");
    }

    private String resolveSectionGuide(String question) {
        if (isObjectiveMethodResultMeaningQuestion(question)) {
            return """
                    ## 研究目标
                    （简述研究的主要目的和假设）
                    
                    ## 研究方法
                    （描述研究设计、样本量、干预措施等）
                    
                    ## 主要结果
                    （列出关键发现，引用具体指标和数值）
                    
                    ## 研究意义
                    （总结临床或研究意义）
                    
                    ## 证据与局限
                    （说明证据来源的可靠性及分析局限）
                    """;
        }
        
        String customStructure = extractCustomStructure(question);
        if (StringUtils.hasText(customStructure)) {
            return customStructure;
        }
        
        return """
                ## 研究结论
                （基于证据的核心结论）
                
                ## 证据综合
                （整合RAG检索证据和指标数据）
                
                ## 指标解读
                （关键指标的含义和变化趋势）
                
                ## 风险与局限
                （数据缺失、证据强度等限制）
                
                ## 下一步建议
                （验证和深入分析建议）
                """;
    }

    private String extractCustomStructure(String question) {
        if (!StringUtils.hasText(question)) {
            return null;
        }
        
        java.util.List<String> sections = new java.util.ArrayList<>();
        
        if (question.contains("疗效")) {
            sections.add("""
                    ## 疗效分析
                    （总结主要疗效指标的变化，包括病毒抑制率、抗原转阴率等，引用具体数值和百分比）
                    """);
        }
        if (question.contains("安全性")) {
            sections.add("""
                    ## 安全性评估
                    （总结不良事件发生率、肝功能指标变化、耐受性等安全性数据）
                    """);
        }
        if (question.contains("关键指标") || question.contains("指标变化")) {
            sections.add("""
                    ## 关键指标变化
                    （按时间点和队列分组，对比分析HBsAg、HBV DNA、ALT、AST等关键指标的变化趋势）
                    """);
        }
        if (question.contains("后续验证") || question.contains("验证建议") || question.contains("建议")) {
            sections.add("""
                    ## 后续验证建议
                    （基于当前研究结果，提出下一步研究方向、样本量建议、联合治疗策略等）
                    """);
        }
        
        if (sections.isEmpty()) {
            return null;
        }
        
        return String.join("\n", sections);
    }

    private boolean isObjectiveMethodResultMeaningQuestion(String question) {
        return StringUtils.hasText(question)
                && question.contains("研究目标")
                && question.contains("研究方法")
                && question.contains("主要结果")
                && (question.contains("研究意义") || question.contains("意义"));
    }

    private boolean containsChinese(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        return text.codePoints().anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private String extractContextLine(String context, String prefix, String fallback) {
        if (!StringUtils.hasText(context) || !StringUtils.hasText(prefix)) {
            return fallback;
        }
        String[] lines = context.split("\\R");
        for (String line : lines) {
            String normalized = line == null ? "" : line.trim();
            if (normalized.startsWith(prefix)) {
                String value = normalized.substring(prefix.length()).trim();
                if (StringUtils.hasText(value)) {
                    return value;
                }
            }
        }
        return fallback;
    }

    private String safeValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String trimTo(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
