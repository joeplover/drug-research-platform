package com.aiforaso.platform.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.aiforaso.platform.domain.Literature;
import com.aiforaso.platform.domain.LiteratureChunk;
import com.aiforaso.platform.domain.StudyIndicator;
import com.aiforaso.platform.dto.IndicatorExtractionRequest;
import com.aiforaso.platform.dto.IndicatorView;
import com.aiforaso.platform.repository.LiteratureChunkRepository;
import com.aiforaso.platform.repository.StudyIndicatorRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class KnowledgeExtractionService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeExtractionService.class);
    private static final String OVERVIEW_CACHE = "literature-overview";

    private static final Pattern DECIMAL_PATTERN = Pattern.compile("[-+]?\\d+(?:\\.\\d+)?");
    private static final Pattern ENGLISH_WEEK_PATTERN = Pattern.compile("W\\d+|week\\s*\\d+|\\d+\\s*weeks", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHINESE_WEEK_PATTERN = Pattern.compile("第\\s*\\d+\\s*周|\\d+\\s*周");
    private static final Pattern ENGLISH_COHORT_PATTERN = Pattern.compile("(3mg|150mg|300mg|placebo|overall|combined)", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHINESE_COHORT_PATTERN = Pattern.compile("(高剂量组|低剂量组|对照组|治疗组|观察组|总体人群|全体受试者)");
    private static final Pattern VALUE_KEYWORD_PATTERN = Pattern.compile("(下降|升高|恢复|比例为|发生率|平均|均|值[为是]?)[^\\d]*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);

    private final StudyIndicatorRepository studyIndicatorRepository;
    private final LiteratureChunkRepository literatureChunkRepository;
    private final LiteratureService literatureService;
    private final LlmService llmService;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    public KnowledgeExtractionService(
            StudyIndicatorRepository studyIndicatorRepository,
            LiteratureChunkRepository literatureChunkRepository,
            LiteratureService literatureService,
            LlmService llmService,
            ObjectMapper objectMapper,
            CacheManager cacheManager) {
        this.studyIndicatorRepository = studyIndicatorRepository;
        this.literatureChunkRepository = literatureChunkRepository;
        this.literatureService = literatureService;
        this.llmService = llmService;
        this.objectMapper = objectMapper;
        this.cacheManager = cacheManager;
    }

    @CacheEvict(cacheNames = OVERVIEW_CACHE, key = "#request.literatureId()")
    @Transactional
    public List<IndicatorView> extractIndicators(IndicatorExtractionRequest request) {
        if (request.literatureId() == null) {
            throw new IllegalArgumentException("literatureId is required for persistent extraction");
        }
        var literature = literatureService.getEntity(request.literatureId());
        String content = StringUtils.hasText(request.content()) ? request.content() : literature.getSummary();
        String cohort = StringUtils.hasText(request.cohort()) ? request.cohort() : "overall";
        String timeWindow = StringUtils.hasText(request.timeWindow()) ? request.timeWindow() : "unspecified";

        List<IndicatorView> llmResults = tryLlmExtraction(request.literatureId(), content, cohort, timeWindow);
        if (!llmResults.isEmpty()) {
            return llmResults;
        }

        List<IndicatorView> results = new ArrayList<>();
        for (IndicatorRule rule : indicatorRules()) {
            String matchedAlias = findMatchedAlias(content, rule.aliases());
            if (matchedAlias == null) {
                continue;
            }

            BigDecimal observedValue = extractObservedValue(content, matchedAlias);
            String evidenceSnippet = extractSnippet(content, matchedAlias);

            StudyIndicator indicator = new StudyIndicator();
            indicator.setLiterature(literature);
            indicator.setIndicatorName(rule.canonicalName());
            indicator.setCategory(rule.category());
            indicator.setTimeWindow(timeWindow);
            indicator.setCohort(cohort);
            indicator.setObservedValue(observedValue);
            indicator.setConfidenceScore(new BigDecimal("0.82"));
            indicator.setEvidenceSnippet(evidenceSnippet);
            indicator.setEvidenceLocator("direct-input");
            indicator.setReviewStatus("待复核");
            results.add(toView(studyIndicatorRepository.save(indicator)));
        }
        return results;
    }

    @Transactional(readOnly = true)
    public List<IndicatorView> listByLiterature(Long literatureId) {
        return studyIndicatorRepository.findByLiteratureId(literatureId).stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<IndicatorView> listByLiteraturePaginated(Long literatureId, int page, int size) {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
        return studyIndicatorRepository.findByLiteratureId(literatureId, pageable).map(this::toView);
    }

    @Transactional(readOnly = true)
    public List<IndicatorView> listConfirmedByLiterature(Long literatureId) {
        return studyIndicatorRepository.findByLiteratureIdAndReviewStatusIgnoreCase(literatureId, "已确认").stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public com.aiforaso.platform.dto.IndicatorReviewSummaryView reviewSummary() {
        long confirmed = studyIndicatorRepository.countByReviewStatusIgnoreCase("已确认");
        long rejected = studyIndicatorRepository.countByReviewStatusIgnoreCase("已拒绝");
        long pending = studyIndicatorRepository.countByReviewStatusIgnoreCase("待复核")
                + studyIndicatorRepository.findAll().stream().filter(item -> item.getReviewStatus() == null || item.getReviewStatus().isBlank()).count();
        long total = studyIndicatorRepository.count();
        return new com.aiforaso.platform.dto.IndicatorReviewSummaryView(total, pending, confirmed, rejected);
    }

    @Transactional
    public IndicatorView reviewIndicator(Long indicatorId, String reviewStatus, String reviewerNote) {
        StudyIndicator indicator = studyIndicatorRepository.findById(indicatorId)
                .orElseThrow(() -> new IllegalArgumentException("Indicator not found: " + indicatorId));
        indicator.setReviewStatus(reviewStatus);
        indicator.setReviewerNote(reviewerNote);
        indicator.setReviewedAt(LocalDateTime.now());
        IndicatorView result = toView(studyIndicatorRepository.save(indicator));
        evictOverviewCache(result.literatureId());
        return result;
    }

    @CacheEvict(cacheNames = OVERVIEW_CACHE, key = "#literatureId")
    @Transactional
    public int reviewAllByLiterature(Long literatureId, String reviewStatus, String reviewerNote) {
        List<StudyIndicator> indicators = studyIndicatorRepository.findByLiteratureId(literatureId);
        int count = 0;
        for (StudyIndicator indicator : indicators) {
            indicator.setReviewStatus(reviewStatus);
            indicator.setReviewerNote(reviewerNote);
            indicator.setReviewedAt(LocalDateTime.now());
            count++;
        }
        studyIndicatorRepository.saveAll(indicators);
        return count;
    }

    @CacheEvict(cacheNames = OVERVIEW_CACHE, key = "#literatureId")
    @Transactional
    public List<IndicatorView> extractIndicatorsByChunks(Long literatureId) {
        var literature = literatureService.getEntity(literatureId);
        List<LiteratureChunk> chunks = literatureChunkRepository.findByLiteratureIdOrderByChunkIndexAsc(literatureId);
        studyIndicatorRepository.deleteByLiteratureId(literatureId);
        if (chunks.isEmpty()) {
            log.warn("[KnowledgeExtraction] No chunks found for literature {}. Falling back to direct text extraction.", literatureId);
            return extractIndicators(new IndicatorExtractionRequest(
                    literatureId,
                    literature.getSummary(),
                    "overall",
                    "unspecified"));
        }

        String combinedContent = chunks.stream()
                .map(LiteratureChunk::getContent)
                .filter(StringUtils::hasText)
                .reduce("", (a, b) -> a + "\n" + b)
                .trim();

        List<IndicatorView> llmResults = tryLlmExtractionFromChunks(literatureId, chunks);
        if (!llmResults.isEmpty()) {
            log.info("[KnowledgeExtraction] LLM extracted {} indicators for literature {}", llmResults.size(), literatureId);
            return llmResults;
        }

        log.info("[KnowledgeExtraction] Falling back to rule-based extraction for literature {}", literatureId);
        List<IndicatorView> ruleResults = extractByRules(literature, chunks);
        if (!ruleResults.isEmpty()) {
            return ruleResults;
        }

        if (StringUtils.hasText(combinedContent)) {
            log.warn("[KnowledgeExtraction] Chunk-based extraction returned empty for literature {}. Falling back to combined text extraction.", literatureId);
            return extractIndicators(new IndicatorExtractionRequest(
                    literatureId,
                    combinedContent,
                    inferCohort(combinedContent),
                    inferTimeWindow(combinedContent)));
        }

        log.warn("[KnowledgeExtraction] Combined chunk content is empty for literature {}. Falling back to literature summary extraction.", literatureId);
        return extractIndicators(new IndicatorExtractionRequest(
                literatureId,
                literature.getSummary(),
                "overall",
                "unspecified"));
    }

    private List<IndicatorView> tryLlmExtraction(Long literatureId, String content, String defaultCohort, String defaultTimeWindow) {
        if (!StringUtils.hasText(content) || content.length() < 50) {
            return List.of();
        }

        String truncatedContent = content.length() > 2500 ? content.substring(0, 2500) : content;

        String systemPrompt = """
                你是一个医学文献指标抽取专家。请从给定文本中抽取所有可量化的研究指标。
                
                你必须返回一个JSON数组，每个元素包含：
                - indicatorName: 指标名称（如 HBsAg、HBV DNA、ALT、AST 等）
                - category: 指标类别（serology/virology/safety/efficacy 之一）
                - timeWindow: 观察时间点（如 第12周、第24周、baseline）
                - cohort: 研究队列（如 治疗组、对照组、overall）
                - observedValue: 观测值（数字，如无则为null）
                - evidenceSnippet: 证据片段（原文中支持该指标的句子，50-100字）
                
                要求：
                1. 只返回JSON数组，不要有其他文字
                2. 指标名称使用标准医学术语
                3. 数值必须是数字类型
                4. 如果文本中没有明确的指标，返回空数组 []
                """;

        String userPrompt = String.format("""
                请从以下文本中抽取研究指标：
                
                %s
                
                默认队列：%s
                默认时间点：%s
                """, truncatedContent, defaultCohort, defaultTimeWindow);

        String response = llmService.chatWithJson(systemPrompt, userPrompt);
        if (!StringUtils.hasText(response)) {
            return List.of();
        }

        return parseLlmIndicators(response, literatureId, "llm-direct");
    }

    private List<IndicatorView> tryLlmExtractionFromChunks(Long literatureId, List<LiteratureChunk> chunks) {
        List<IndicatorView> allResults = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();

        for (LiteratureChunk chunk : chunks) {
            String content = chunk.getContent();
            if (!StringUtils.hasText(content) || content.length() < 30) {
                continue;
            }

            String truncatedContent = content.length() > 1500 ? content.substring(0, 1500) : content;

            String systemPrompt = """
                    你是一个医学文献指标抽取专家。请从给定文本片段中抽取所有可量化的研究指标。
                    
                    你必须返回一个JSON数组，每个元素包含：
                    - indicatorName: 指标名称（如 HBsAg、HBV DNA、ALT、AST 等）
                    - category: 指标类别（serology/virology/safety/efficacy 之一）
                    - timeWindow: 观察时间点（如 第12周、第24周、baseline，如无则填 "unspecified"）
                    - cohort: 研究队列（如 治疗组、对照组、overall，如无则填 "overall"）
                    - observedValue: 观测值（数字，如无则为null）
                    - evidenceSnippet: 证据片段（原文中支持该指标的句子，30-80字）
                    
                    要求：
                    1. 只返回JSON数组，不要有其他文字
                    2. 指标名称使用标准医学术语
                    3. 数值必须是数字类型
                    4. 如果没有明确指标，返回空数组 []
                    5. 使用中文回答
                    """;

            String userPrompt = String.format("""
                    请从以下文本片段中抽取研究指标：
                    
                    %s
                    """, truncatedContent);

            String response = llmService.chatWithJson(systemPrompt, userPrompt);
            if (!StringUtils.hasText(response)) {
                continue;
            }

            List<IndicatorView> chunkResults = parseLlmIndicators(response, literatureId, chunk.getChunkLabel());
            for (IndicatorView indicator : chunkResults) {
                String key = indicator.indicatorName() + "|" + indicator.timeWindow() + "|" + indicator.cohort();
                if (seenKeys.add(key)) {
                    allResults.add(indicator);
                }
            }

            if (allResults.size() >= 50) {
                break;
            }
        }

        return allResults;
    }

    private List<IndicatorView> parseLlmIndicators(String response, Long literatureId, String evidenceLocator) {
        try {
            String jsonStr = extractJson(response);
            JsonNode array = objectMapper.readTree(jsonStr);

            if (!array.isArray()) {
                return List.of();
            }

            var literature = literatureService.getEntity(literatureId);
            List<IndicatorView> results = new ArrayList<>();

            for (JsonNode node : array) {
                try {
                    String indicatorName = getTextOrDefault(node, "indicatorName", null);
                    if (!StringUtils.hasText(indicatorName)) {
                        continue;
                    }

                    String category = getTextOrDefault(node, "category", "safety");
                    String timeWindow = getTextOrDefault(node, "timeWindow", "unspecified");
                    String cohort = getTextOrDefault(node, "cohort", "overall");
                    String evidenceSnippet = getTextOrDefault(node, "evidenceSnippet", "");

                    BigDecimal observedValue = null;
                    if (node.has("observedValue") && !node.get("observedValue").isNull()) {
                        try {
                            observedValue = new BigDecimal(node.get("observedValue").asText());
                        } catch (NumberFormatException ignored) {
                        }
                    }

                    StudyIndicator indicator = new StudyIndicator();
                    indicator.setLiterature(literature);
                    indicator.setIndicatorName(indicatorName);
                    indicator.setCategory(category);
                    indicator.setTimeWindow(timeWindow);
                    indicator.setCohort(cohort);
                    indicator.setObservedValue(observedValue);
                    indicator.setConfidenceScore(new BigDecimal("0.85"));
                    indicator.setEvidenceSnippet(evidenceSnippet);
                    indicator.setEvidenceLocator(evidenceLocator);
                    indicator.setReviewStatus("待复核");
                    results.add(toView(studyIndicatorRepository.save(indicator)));
                } catch (Exception e) {
                    log.warn("[KnowledgeExtraction] Failed to parse indicator: {}", e.getMessage());
                }
            }

            return results;
        } catch (Exception e) {
            log.warn("[KnowledgeExtraction] Failed to parse LLM response: {}", e.getMessage());
            return List.of();
        }
    }

    private List<IndicatorView> extractByRules(Literature literature, List<LiteratureChunk> chunks) {
        List<IndicatorView> results = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        for (LiteratureChunk chunk : chunks) {
            String content = chunk.getContent();
            for (IndicatorRule rule : indicatorRules()) {
                String matchedAlias = findMatchedAlias(content, rule.aliases());
                if (matchedAlias == null) {
                    continue;
                }

                String timeWindow = inferTimeWindowNearIndicator(content, matchedAlias);
                String cohort = inferCohortNearIndicator(content, matchedAlias);
                BigDecimal observedValue = extractObservedValue(content, matchedAlias);
                String evidenceSnippet = extractSnippet(content, matchedAlias);
                String evidenceLocator = chunk.getChunkLabel();
                String dedupeKey = buildDedupeKey(rule.canonicalName(), timeWindow, cohort, observedValue, evidenceLocator);
                if (!seenKeys.add(dedupeKey)) {
                    continue;
                }

                StudyIndicator indicator = new StudyIndicator();
                indicator.setLiterature(literature);
                indicator.setIndicatorName(rule.canonicalName());
                indicator.setCategory(rule.category());
                indicator.setTimeWindow(timeWindow);
                indicator.setCohort(cohort);
                indicator.setObservedValue(observedValue);
                indicator.setConfidenceScore(new BigDecimal("0.82"));
                indicator.setEvidenceSnippet(evidenceSnippet);
                indicator.setEvidenceLocator(evidenceLocator);
                indicator.setReviewStatus("待复核");
                results.add(toView(studyIndicatorRepository.save(indicator)));
            }
        }
        return results;
    }

    private List<IndicatorRule> indicatorRules() {
        return List.of(
                new IndicatorRule("HBsAg", "serology", List.of("HBsAg", "表面抗原")),
                new IndicatorRule("HBV DNA", "virology", List.of("HBV DNA", "病毒核酸载量", "病毒载量")),
                new IndicatorRule("pgRNA", "virology", List.of("pgRNA")),
                new IndicatorRule("ALT", "safety", List.of("ALT", "丙氨酸氨基转移酶")),
                new IndicatorRule("AST", "safety", List.of("AST", "天门冬氨酸氨基转移酶")),
                new IndicatorRule("HBeAg", "serology", List.of("HBeAg", "e抗原")),
                new IndicatorRule("不良事件", "safety", List.of("不良事件", "严重不良事件", "adverse event", "adverse events")));
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }

    private String findMatchedAlias(String content, List<String> aliases) {
        if (!StringUtils.hasText(content) || aliases == null || aliases.isEmpty()) {
            return null;
        }
        for (String alias : aliases) {
            if (containsIgnoreCase(content, alias)) {
                return alias;
            }
        }
        return null;
    }

    private BigDecimal extractObservedValue(String content, String indicatorName) {
        if (content == null || content.isBlank()) {
            return null;
        }
        int index = content.toLowerCase().indexOf(indicatorName.toLowerCase());
        if (index < 0) {
            return null;
        }
        String forwardWindow = content.substring(index, Math.min(content.length(), index + 120));
        Matcher valueKeywordMatcher = VALUE_KEYWORD_PATTERN.matcher(forwardWindow);
        if (valueKeywordMatcher.find()) {
            String valueStr = valueKeywordMatcher.group(2);
            if (valueStr != null) {
                return new BigDecimal(valueStr);
            }
        }
        Matcher matcher = DECIMAL_PATTERN.matcher(forwardWindow);
        if (matcher.find()) {
            String valueStr = matcher.group();
            if (valueStr != null && valueStr.length() <= 6) {
                return new BigDecimal(valueStr);
            }
        }
        return null;
    }

    private String inferTimeWindow(String content) {
        Matcher chineseMatcher = CHINESE_WEEK_PATTERN.matcher(content);
        if (chineseMatcher.find()) {
            return normalizeTimeWindow(chineseMatcher.group().replaceAll("\\s+", ""));
        }
        Matcher englishMatcher = ENGLISH_WEEK_PATTERN.matcher(content);
        return englishMatcher.find() ? normalizeTimeWindow(englishMatcher.group()) : "unspecified";
    }

    private String inferTimeWindowNearIndicator(String content, String indicatorName) {
        if (content == null || content.isBlank()) {
            return "unspecified";
        }
        int indicatorIndex = content.toLowerCase().indexOf(indicatorName.toLowerCase());
        if (indicatorIndex < 0) {
            return inferTimeWindow(content);
        }
        String beforeIndicator = content.substring(0, indicatorIndex);
        Matcher chineseMatcher = CHINESE_WEEK_PATTERN.matcher(beforeIndicator);
        String lastMatch = null;
        while (chineseMatcher.find()) {
            lastMatch = chineseMatcher.group().replaceAll("\\s+", "");
        }
        if (lastMatch != null) {
            return normalizeTimeWindow(lastMatch);
        }
        Matcher englishMatcher = ENGLISH_WEEK_PATTERN.matcher(beforeIndicator);
        while (englishMatcher.find()) {
            lastMatch = englishMatcher.group();
        }
        return lastMatch != null ? normalizeTimeWindow(lastMatch) : inferTimeWindow(content);
    }

    private String normalizeTimeWindow(String timeWindow) {
        if (timeWindow == null || timeWindow.isBlank()) {
            return "unspecified";
        }
        Matcher numberMatcher = Pattern.compile("\\d+").matcher(timeWindow);
        if (numberMatcher.find()) {
            String weekNumber = numberMatcher.group();
            return "第" + weekNumber + "周";
        }
        return timeWindow;
    }

    private String inferCohort(String content) {
        Matcher chineseMatcher = CHINESE_COHORT_PATTERN.matcher(content);
        if (chineseMatcher.find()) {
            return chineseMatcher.group();
        }
        Matcher englishMatcher = ENGLISH_COHORT_PATTERN.matcher(content);
        return englishMatcher.find() ? englishMatcher.group() : "overall";
    }

    private String inferCohortNearIndicator(String content, String indicatorName) {
        if (content == null || content.isBlank()) {
            return "overall";
        }
        int indicatorIndex = content.toLowerCase().indexOf(indicatorName.toLowerCase());
        if (indicatorIndex < 0) {
            return inferCohort(content);
        }
        String beforeIndicator = content.substring(0, indicatorIndex);
        Matcher chineseMatcher = CHINESE_COHORT_PATTERN.matcher(beforeIndicator);
        String lastMatch = null;
        while (chineseMatcher.find()) {
            lastMatch = chineseMatcher.group();
        }
        if (lastMatch != null) {
            return lastMatch;
        }
        Matcher englishMatcher = ENGLISH_COHORT_PATTERN.matcher(beforeIndicator);
        while (englishMatcher.find()) {
            lastMatch = englishMatcher.group();
        }
        return lastMatch != null ? lastMatch : inferCohort(content);
    }

    private String extractSnippet(String content, String indicatorName) {
        String lower = content.toLowerCase();
        int hitIndex = lower.indexOf(indicatorName.toLowerCase());
        int start = Math.max(0, (hitIndex < 0 ? 0 : hitIndex) - 40);
        int end = Math.min(content.length(), start + 180);
        return content.substring(start, end);
    }

    private String buildDedupeKey(String indicatorName, String timeWindow, String cohort, BigDecimal observedValue, String evidenceLocator) {
        return String.join("|",
                indicatorName == null ? "" : indicatorName,
                timeWindow == null ? "" : timeWindow,
                cohort == null ? "" : cohort,
                observedValue == null ? "" : observedValue.toPlainString(),
                evidenceLocator == null ? "" : evidenceLocator);
    }

    private IndicatorView toView(StudyIndicator indicator) {
        return new IndicatorView(
                indicator.getId(),
                indicator.getLiterature().getId(),
                indicator.getIndicatorName(),
                indicator.getCategory(),
                indicator.getTimeWindow(),
                indicator.getCohort(),
                indicator.getObservedValue(),
                indicator.getConfidenceScore(),
                indicator.getEvidenceSnippet(),
                indicator.getEvidenceLocator(),
                indicator.getReviewStatus(),
                indicator.getReviewerNote(),
                indicator.getReviewedAt());
    }

    private String extractJson(String response) {
        String trimmed = response.trim();
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        start = trimmed.indexOf('{');
        end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return "[" + trimmed.substring(start, end + 1) + "]";
        }
        return "[]";
    }

    private String getTextOrDefault(JsonNode node, String field, String defaultValue) {
        if (node.has(field)) {
            JsonNode fieldNode = node.get(field);
            if (fieldNode.isTextual()) {
                String value = fieldNode.asText();
                return StringUtils.hasText(value) ? value : defaultValue;
            } else if (fieldNode.isNumber()) {
                return fieldNode.asText();
            } else if (!fieldNode.isNull()) {
                log.debug("[KnowledgeExtraction] Field {} is not text or number, type: {}", field, fieldNode.getNodeType());
            }
        }
        return defaultValue;
    }

    private record IndicatorRule(String canonicalName, String category, List<String> aliases) {
    }

    private void evictOverviewCache(Long literatureId) {
        Cache cache = cacheManager.getCache(OVERVIEW_CACHE);
        if (cache != null) {
            cache.evict(literatureId);
            log.debug("[KnowledgeExtraction] Evicted overview cache for literatureId={}", literatureId);
        }
    }
}
