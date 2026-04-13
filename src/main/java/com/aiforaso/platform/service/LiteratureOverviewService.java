package com.aiforaso.platform.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.aiforaso.platform.dto.IndicatorView;
import com.aiforaso.platform.dto.KnowledgeGraphResponse;
import com.aiforaso.platform.dto.LiteratureChunkView;
import com.aiforaso.platform.dto.LiteratureOverviewView;
import com.aiforaso.platform.dto.LiteratureView;

@Service
public class LiteratureOverviewService {

    private static final Logger log = LoggerFactory.getLogger(LiteratureOverviewService.class);
    private static final String CACHE_NAME = "literature-overview";

    private final LiteratureService literatureService;
    private final LiteratureIngestionService literatureIngestionService;
    private final KnowledgeExtractionService knowledgeExtractionService;
    private final KnowledgeGraphService knowledgeGraphService;
    private final DocumentParserService documentParserService;
    private final LiteratureInsightService literatureInsightService;

    public LiteratureOverviewService(
            LiteratureService literatureService,
            LiteratureIngestionService literatureIngestionService,
            KnowledgeExtractionService knowledgeExtractionService,
            KnowledgeGraphService knowledgeGraphService,
            DocumentParserService documentParserService,
            LiteratureInsightService literatureInsightService) {
        this.literatureService = literatureService;
        this.literatureIngestionService = literatureIngestionService;
        this.knowledgeExtractionService = knowledgeExtractionService;
        this.knowledgeGraphService = knowledgeGraphService;
        this.documentParserService = documentParserService;
        this.literatureInsightService = literatureInsightService;
    }

    @Cacheable(cacheNames = CACHE_NAME, key = "#literatureId")
    @Transactional(readOnly = true)
    public LiteratureOverviewView build(Long literatureId) {
        log.debug("[LiteratureOverview] Building overview for literatureId={}", literatureId);
        
        LiteratureView literature = literatureService.get(literatureId);
        List<IndicatorView> indicators = knowledgeExtractionService.listByLiterature(literatureId);
        List<LiteratureChunkView> chunks = literatureIngestionService.list(literatureId);
        var vectorStatus = literatureIngestionService.vectorStatus(literatureId);
        KnowledgeGraphResponse graph = knowledgeGraphService.buildGraphForLiterature(literatureId);

        int chunkCount = vectorStatus.chunkCount() == null ? 0 : vectorStatus.chunkCount();
        int embeddedChunkCount = vectorStatus.embeddedChunkCount() == null ? 0 : vectorStatus.embeddedChunkCount();

        String primaryText = resolvePrimaryText(literature, chunks);
        LiteratureInsightService.LiteratureInsight insight = literatureInsightService.analyze(
                literature.title(),
                literature.diseaseArea(),
                literature.keywords(),
                primaryText,
                indicators.stream().map(IndicatorView::indicatorName).distinct().toList());

        List<String> evidenceHighlights = !indicators.isEmpty()
                ? indicators.stream()
                        .map(IndicatorView::evidenceSnippet)
                        .filter(StringUtils::hasText)
                        .limit(4)
                        .toList()
                : insight.evidenceHighlights();

        return new LiteratureOverviewView(
                literature.id(),
                literature.title(),
                resolveProcessingStage(chunkCount, embeddedChunkCount, indicators.size()),
                chunkCount,
                indicators.size(),
                insight.overviewSummary(),
                insight.researchFocus(),
                insight.methodSummary(),
                insight.resultSummary(),
                insight.safetySummary(),
                insight.conclusionSummary(),
                insight.keyPoints(),
                insight.keyConcepts(),
                evidenceHighlights,
                indicators.stream().limit(6).toList(),
                graph);
    }

    @CacheEvict(cacheNames = CACHE_NAME, key = "#literatureId")
    public void evictCache(Long literatureId) {
        log.debug("[LiteratureOverview] Cache evicted for literatureId={}", literatureId);
    }

    private String resolvePrimaryText(LiteratureView literature, List<LiteratureChunkView> chunks) {
        if (chunks != null && !chunks.isEmpty()) {
            return chunks.stream()
                    .map(LiteratureChunkView::content)
                    .limit(6)
                    .reduce("", (left, right) -> left + "\n" + right)
                    .trim();
        }

        if (StringUtils.hasText(literature.storagePath())) {
            try {
                return documentParserService.extractText(
                        literatureService.resolveStoragePath(literature.storagePath()),
                        literature.sourceType());
            } catch (Exception ignored) {
                return literature.summary();
            }
        }
        return literature.summary();
    }

    private String resolveProcessingStage(int chunkCount, int embeddedChunkCount, int indicatorCount) {
        if (chunkCount <= 0) {
            return "已导入，待解析";
        }
        if (embeddedChunkCount < chunkCount) {
            return "已解析，待完成向量化";
        }
        if (indicatorCount <= 0) {
            return "已向量化，待抽取关键指标";
        }
        return "已完成文献概览准备";
    }
}
