package com.aiforaso.platform.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LiteratureInsightService {

    private static final Logger log = LoggerFactory.getLogger(LiteratureInsightService.class);

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final Pattern SENTENCE_SPLIT = Pattern.compile("(?<=[。！？；.!?;])\\s+");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9\\-]{2,}");

    private final LlmService llmService;
    private final ObjectMapper objectMapper;

    public LiteratureInsightService(LlmService llmService, ObjectMapper objectMapper) {
        this.llmService = llmService;
        this.objectMapper = objectMapper;
    }

    public LiteratureInsight analyze(
            String title,
            String diseaseArea,
            String keywords,
            String text,
            List<String> indicatorNames) {
        
        LiteratureInsight llmInsight = tryLlmAnalysis(title, diseaseArea, keywords, text, indicatorNames);
        if (llmInsight != null) {
            log.debug("[LiteratureInsight] Using LLM-enhanced analysis");
            return llmInsight;
        }

        log.debug("[LiteratureInsight] Falling back to rule-based analysis");
        return analyzeByRules(title, diseaseArea, keywords, text, indicatorNames);
    }

    private LiteratureInsight tryLlmAnalysis(
            String title,
            String diseaseArea,
            String keywords,
            String text,
            List<String> indicatorNames) {
        
        String truncatedText = truncate(text, 3000);
        if (!StringUtils.hasText(truncatedText)) {
            return null;
        }

        String systemPrompt = """
                你是一个医学文献分析专家。请分析给定的文献内容，提取结构化信息。
                你必须返回一个JSON对象，包含以下字段：
                - overviewSummary: 文献整体概述（100-150字）
                - researchFocus: 研究目标/目的（50-80字）
                - methodSummary: 研究方法概述（50-80字）
                - resultSummary: 主要研究结果（80-120字）
                - safetySummary: 安全性相关发现（50-80字，如无则说明）
                - conclusionSummary: 研究结论和意义（50-80字）
                - keyPoints: 关键要点列表（3-5条，每条20-40字）
                - keyConcepts: 关键概念/术语列表（5-8个）
                - evidenceHighlights: 重要证据片段（2-4条，每条30-60字）
                
                要求：
                1. 内容必须基于原文，不可编造
                2. 使用中文回答
                3. 返回纯JSON格式，不要有其他文字
                """;

        String userPrompt = String.format("""
                文献标题：%s
                疾病领域：%s
                关键词：%s
                已抽取指标：%s
                
                文献内容：
                %s
                
                请分析并返回JSON格式的结构化信息。
                """,
                safeValue(title, "未知"),
                safeValue(diseaseArea, "未指定"),
                safeValue(keywords, "无"),
                indicatorNames == null ? "无" : String.join("、", indicatorNames),
                truncatedText);

        String response = llmService.chatWithJson(systemPrompt, userPrompt);
        if (!StringUtils.hasText(response)) {
            return null;
        }

        try {
            String jsonStr = extractJson(response);
            JsonNode node = objectMapper.readTree(jsonStr);

            return new LiteratureInsight(
                    getTextOrDefault(node, "overviewSummary", ""),
                    getTextOrDefault(node, "researchFocus", ""),
                    getTextOrDefault(node, "methodSummary", ""),
                    getTextOrDefault(node, "resultSummary", ""),
                    getTextOrDefault(node, "safetySummary", "未识别到明确的安全性信息。"),
                    getTextOrDefault(node, "conclusionSummary", ""),
                    getListOrDefault(node, "keyPoints"),
                    getListOrDefault(node, "keyConcepts"),
                    getListOrDefault(node, "evidenceHighlights"));
        } catch (Exception e) {
            log.warn("[LiteratureInsight] Failed to parse LLM response: {}", e.getMessage());
            return null;
        }
    }

    public LiteratureInsight analyzeByRules(
            String title,
            String diseaseArea,
            String keywords,
            String text,
            List<String> indicatorNames) {
        String normalized = normalize(text);
        List<String> sentences = splitSentences(normalized);

        String researchFocus = pickSection(sentences,
                List.of("目的", "研究", "aim", "objective", "investigate", "evaluate", "assess"),
                firstSentence(sentences, normalized));
        String methodSummary = pickSection(sentences,
                List.of("方法", "method", "design", "trial", "study", "dose", "treatment", "cohort", "randomized"),
                fallbackByOrder(sentences, 1, normalized));
        String resultSummary = pickSection(sentences,
                List.of("结果", "result", "change", "reduction", "decrease", "increase", "response", "observed", "improved"),
                fallbackByOrder(sentences, 2, normalized));
        String safetySummary = pickSection(sentences,
                List.of("安全", "不良", "safety", "adverse", "tolerability", "ALT", "AST"),
                "当前文本中未识别出明确的安全性总结，可结合原文进一步复核。");
        String conclusionSummary = pickSection(sentences,
                List.of("结论", "提示", "说明", "conclusion", "suggest", "indicate", "support"),
                lastSentence(sentences, normalized));

        List<String> keyPoints = distinctNonBlank(List.of(
                researchFocus,
                methodSummary,
                resultSummary,
                conclusionSummary));

        List<String> keyConcepts = buildConcepts(title, diseaseArea, keywords, normalized, indicatorNames);
        List<String> evidenceHighlights = buildEvidenceHighlights(sentences, indicatorNames);

        String overviewSummary = truncate(String.join(" ", distinctNonBlank(List.of(
                researchFocus,
                resultSummary,
                conclusionSummary))), 1200);
        if (!StringUtils.hasText(overviewSummary)) {
            overviewSummary = truncate(normalized, 1200);
        }

        return new LiteratureInsight(
                overviewSummary,
                researchFocus,
                methodSummary,
                resultSummary,
                safetySummary,
                conclusionSummary,
                keyPoints,
                keyConcepts,
                evidenceHighlights);
    }

    private String normalize(String text) {
        return WHITESPACE.matcher(text == null ? "" : text).replaceAll(" ").trim();
    }

    private List<String> splitSentences(String normalized) {
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }
        List<String> sentences = new ArrayList<>();
        for (String item : SENTENCE_SPLIT.split(normalized)) {
            String candidate = item == null ? "" : item.trim();
            if (candidate.length() >= 12) {
                sentences.add(candidate);
            }
        }
        if (sentences.isEmpty() && StringUtils.hasText(normalized)) {
            sentences.add(normalized);
        }
        return sentences;
    }

    private String pickSection(List<String> sentences, List<String> keywords, String fallback) {
        if (sentences.isEmpty()) {
            return fallback;
        }

        List<String> matches = new ArrayList<>();
        for (String sentence : sentences) {
            String lower = sentence.toLowerCase(Locale.ROOT);
            boolean matched = keywords.stream().anyMatch(keyword -> lower.contains(keyword.toLowerCase(Locale.ROOT)));
            if (matched) {
                matches.add(sentence);
            }
            if (matches.size() >= 2) {
                break;
            }
        }

        if (!matches.isEmpty()) {
            return truncate(String.join(" ", matches), 420);
        }
        return fallback;
    }

    private String firstSentence(List<String> sentences, String normalized) {
        if (!sentences.isEmpty()) {
            return truncate(sentences.get(0), 320);
        }
        return truncate(normalized, 320);
    }

    private String fallbackByOrder(List<String> sentences, int index, String normalized) {
        if (sentences.size() > index) {
            return truncate(sentences.get(index), 320);
        }
        return truncate(normalized, 320);
    }

    private String lastSentence(List<String> sentences, String normalized) {
        if (!sentences.isEmpty()) {
            return truncate(sentences.get(sentences.size() - 1), 320);
        }
        return truncate(normalized, 320);
    }

    private List<String> buildConcepts(
            String title,
            String diseaseArea,
            String keywords,
            String normalized,
            List<String> indicatorNames) {
        LinkedHashSet<String> concepts = new LinkedHashSet<>();
        addConcept(concepts, title);
        addConcept(concepts, diseaseArea);

        if (StringUtils.hasText(keywords)) {
            for (String item : keywords.split("[,，;；/|]")) {
                addConcept(concepts, item);
            }
        }

        if (indicatorNames != null) {
            indicatorNames.forEach(item -> addConcept(concepts, item));
        }

        var matcher = TOKEN_PATTERN.matcher(normalized);
        while (matcher.find() && concepts.size() < 12) {
            String token = matcher.group();
            if (token.length() >= 3 && Character.isUpperCase(token.charAt(0))) {
                addConcept(concepts, token);
            }
        }
        return new ArrayList<>(concepts).stream().limit(12).toList();
    }

    private List<String> buildEvidenceHighlights(List<String> sentences, List<String> indicatorNames) {
        List<String> highlights = new ArrayList<>();
        if (indicatorNames != null) {
            for (String indicatorName : indicatorNames) {
                for (String sentence : sentences) {
                    if (sentence.toLowerCase(Locale.ROOT).contains(indicatorName.toLowerCase(Locale.ROOT))) {
                        highlights.add(truncate(sentence, 220));
                        if (highlights.size() >= 4) {
                            return distinctNonBlank(highlights);
                        }
                    }
                }
            }
        }

        for (String sentence : sentences) {
            highlights.add(truncate(sentence, 220));
            if (highlights.size() >= 4) {
                break;
            }
        }
        return distinctNonBlank(highlights);
    }

    private List<String> distinctNonBlank(List<String> values) {
        Set<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                unique.add(value.trim());
            }
        }
        return new ArrayList<>(unique);
    }

    private void addConcept(Set<String> concepts, String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return;
        }
        String value = rawValue.trim();
        if (value.length() > 40) {
            value = value.substring(0, 40);
        }
        concepts.add(value);
    }

    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String safeValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String extractJson(String response) {
        String trimmed = response.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String getTextOrDefault(JsonNode node, String field, String defaultValue) {
        if (node.has(field) && node.get(field).isTextual()) {
            String value = node.get(field).asText();
            return StringUtils.hasText(value) ? value : defaultValue;
        }
        return defaultValue;
    }

    private List<String> getListOrDefault(JsonNode node, String field) {
        if (node.has(field) && node.get(field).isArray()) {
            List<String> result = new ArrayList<>();
            for (JsonNode item : node.get(field)) {
                if (item.isTextual()) {
                    String text = item.asText();
                    if (StringUtils.hasText(text)) {
                        result.add(text);
                    }
                }
            }
            return result;
        }
        return List.of();
    }

    public record LiteratureInsight(
            String overviewSummary,
            String researchFocus,
            String methodSummary,
            String resultSummary,
            String safetySummary,
            String conclusionSummary,
            List<String> keyPoints,
            List<String> keyConcepts,
            List<String> evidenceHighlights) {
    }
}
