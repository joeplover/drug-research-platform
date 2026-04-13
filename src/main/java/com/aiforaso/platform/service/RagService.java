package com.aiforaso.platform.service;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.aiforaso.platform.domain.LiteratureChunk;
import com.aiforaso.platform.dto.RagHitView;
import com.aiforaso.platform.dto.RagQueryPageRequest;
import com.aiforaso.platform.dto.RagQueryRequest;
import com.aiforaso.platform.repository.LiteratureChunkRepository;

@Service
public class RagService {

    private static final Pattern HAN_SEGMENT_PATTERN = Pattern.compile("[\\p{IsHan}]{2,}");
    private static final List<String> CHINESE_HINT_TOKENS = List.of(
            "研究目标", "研究方法", "主要结果", "研究意义", "目的", "方法", "结果", "结论", "意义",
            "疗效", "安全性", "不良事件", "表面抗原", "病毒核酸载量", "病毒载量");

    private final LiteratureService literatureService;
    private final LiteratureChunkRepository literatureChunkRepository;
    private final ChunkEmbeddingService chunkEmbeddingService;
    private final MilvusVectorStoreService milvusVectorStoreService;

    public RagService(
            LiteratureService literatureService,
            LiteratureChunkRepository literatureChunkRepository,
            ChunkEmbeddingService chunkEmbeddingService,
            MilvusVectorStoreService milvusVectorStoreService) {
        this.literatureService = literatureService;
        this.literatureChunkRepository = literatureChunkRepository;
        this.chunkEmbeddingService = chunkEmbeddingService;
        this.milvusVectorStoreService = milvusVectorStoreService;
    }

    @Cacheable(cacheNames = "rag-results", key = "#request.query() + ':' + (#request.topK() == null ? 5 : #request.topK()) + ':' + (#request.literatureIds() == null ? 'all' : #request.literatureIds().toString())")
    public List<RagHitView> retrieve(RagQueryRequest request) {
        int limit = request.topK() == null ? 5 : request.topK();
        List<String> queryTokens = tokenize(request.query());
        double[] queryVector = chunkEmbeddingService.embed(request.query());
        Set<Long> scopedLiteratureIds = request.literatureIds() == null || request.literatureIds().isEmpty()
                ? Set.of()
                : new HashSet<>(request.literatureIds());
        List<RagHitView> milvusHits = retrieveFromMilvus(queryTokens, queryVector, limit, scopedLiteratureIds);
        if (!milvusHits.isEmpty()) {
            return milvusHits;
        }

        List<LiteratureChunk> chunks = literatureChunkRepository.findAllWithLiterature().stream()
                .filter(chunk -> scopedLiteratureIds.isEmpty() || scopedLiteratureIds.contains(chunk.getLiterature().getId()))
                .toList();

        Stream<RagHitView> searchStream = chunks.isEmpty()
                ? literatureService.list(null).stream()
                    .filter(view -> scopedLiteratureIds.isEmpty() || scopedLiteratureIds.contains(view.id()))
                    .map(view -> {
                    var literature = literatureService.getEntity(view.id());
                    double score = calculateScore(queryTokens,
                            literature.getTitle(), literature.getSummary(), literature.getKeywords());
                    return new RagHitView(
                            literature.getId(),
                            literature.getTitle(),
                            buildSnippet(literature.getSummary(), request.query()),
                            score,
                            literature.getSourceType(),
                            literature.getPublicationDate() == null ? "" : literature.getPublicationDate().format(DateTimeFormatter.ISO_DATE),
                            "summary",
                            "LITERATURE_SCAN");
                })
                : chunks.stream().map(chunk -> {
                    var literature = chunk.getLiterature();
                    double score = calculateHybridScore(queryTokens, queryVector, chunk,
                            literature.getTitle(), chunk.getContent(), chunk.getChunkLabel(), literature.getKeywords());
                    return new RagHitView(
                            literature.getId(),
                            literature.getTitle(),
                            buildSnippet(chunk.getContent(), request.query()),
                            score,
                            literature.getSourceType(),
                            literature.getPublicationDate() == null ? "" : literature.getPublicationDate().format(DateTimeFormatter.ISO_DATE),
                            chunk.getChunkLabel(),
                            "LOCAL_HYBRID");
                });

        return searchStream
                .filter(hit -> hit.score() > 0)
                .sorted(Comparator.comparingDouble(RagHitView::score).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Page<RagHitView> retrievePaginated(RagQueryPageRequest request) {
        int topK = request.topK() == null ? 50 : request.topK();
        int page = request.page() == null ? 1 : request.page();
        int size = request.size() == null ? 10 : request.size();

        RagQueryRequest baseRequest = new RagQueryRequest(request.query(), topK, request.literatureIds());
        List<RagHitView> allHits = retrieve(baseRequest);

        int start = (page - 1) * size;
        int end = Math.min(start + size, allHits.size());
        List<RagHitView> pageContent = start < allHits.size() ? allHits.subList(start, end) : List.of();

        return new PageImpl<>(pageContent, PageRequest.of(page - 1, size), allHits.size());
    }

    private List<RagHitView> retrieveFromMilvus(List<String> queryTokens, double[] queryVector, int limit, Set<Long> scopedLiteratureIds) {
        if (!milvusVectorStoreService.isEnabled()) {
            return List.of();
        }

        List<io.milvus.v2.service.vector.response.SearchResp.SearchResult> searchResults =
                milvusVectorStoreService.search(queryVector, scopedLiteratureIds.isEmpty() ? Math.max(limit * 4, 20) : Math.max(limit * 20, 100));
        if (searchResults.isEmpty()) {
            return List.of();
        }

        List<Long> chunkIds = searchResults.stream()
                .map(result -> Long.valueOf(String.valueOf(result.getId())))
                .toList();
        Map<Long, LiteratureChunk> chunkMap = new LinkedHashMap<>();
        literatureChunkRepository.findAllByIdWithLiterature(chunkIds).forEach(chunk -> chunkMap.put(chunk.getId(), chunk));

        return searchResults.stream()
                .map(result -> {
                    Long chunkId = Long.valueOf(String.valueOf(result.getId()));
                    LiteratureChunk chunk = chunkMap.get(chunkId);
                    if (chunk == null) {
                        return null;
                    }
                    if (!scopedLiteratureIds.isEmpty() && !scopedLiteratureIds.contains(chunk.getLiterature().getId())) {
                        return null;
                    }
                    var literature = chunk.getLiterature();
                    double lexical = calculateScore(queryTokens,
                            literature.getTitle(), chunk.getContent(), chunk.getChunkLabel(), literature.getKeywords());
                    double semantic = result.getScore() == null ? 0.0 : result.getScore();
                    if (semantic < SEMANTIC_THRESHOLD && lexical <= 0) {
                        return null;
                    }
                    return new RagHitView(
                            literature.getId(),
                            literature.getTitle(),
                            buildSnippet(chunk.getContent(), String.join(" ", queryTokens)),
                            lexical + semantic,
                            literature.getSourceType(),
                            literature.getPublicationDate() == null ? "" : literature.getPublicationDate().format(DateTimeFormatter.ISO_DATE),
                            chunk.getChunkLabel(),
                            "MILVUS_HYBRID");
                })
                .filter(hit -> hit != null)
                .filter(hit -> hit.score() > 0)
                .sorted(Comparator.comparingDouble(RagHitView::score).reversed())
                .limit(limit)
                .toList();
    }

    private List<String> tokenize(String query) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        Arrays.stream(query.toLowerCase(Locale.ROOT).split("\\s+"))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .forEach(tokens::add);

        String normalized = query.replaceAll("[，。；、：！？,.!?()（）“”\"'\\[\\]{}]", " ");
        Matcher matcher = HAN_SEGMENT_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String segment = matcher.group().trim();
            if (segment.length() >= 2) {
                tokens.add(segment);
            }
        }

        for (String hint : CHINESE_HINT_TOKENS) {
            if (query.contains(hint)) {
                tokens.add(hint.toLowerCase(Locale.ROOT));
            }
        }

        return tokens.stream()
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
    }

    private double calculateScore(List<String> queryTokens, String... fields) {
        String targetText = Arrays.stream(fields)
                .map(value -> value == null ? "" : value)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .reduce("", (left, right) -> left + " " + right);
        return queryTokens.stream()
                .mapToDouble(token -> {
                    if (targetText.contains(token)) {
                        return token.length() >= 4 ? 1.5 : 1.0;
                    }
                    return 0.0;
                })
                .sum();
    }

    private static final double SEMANTIC_THRESHOLD = 0.3;

    private double calculateHybridScore(List<String> queryTokens, double[] queryVector, LiteratureChunk chunk, String... fields) {
        double lexical = calculateScore(queryTokens, fields);
        double[] chunkVector = chunkEmbeddingService.cosineSourceVector(chunk.getContent(), chunk.getEmbeddingJson());
        double semantic = chunkEmbeddingService.cosineSimilarity(queryVector, chunkVector);
        if (semantic < SEMANTIC_THRESHOLD && lexical <= 0) {
            return 0.0;
        }
        return lexical + semantic;
    }

    private String buildSnippet(String summary, String query) {
        if (summary == null || summary.isBlank()) {
            return "";
        }
        String lowerSummary = summary.toLowerCase(Locale.ROOT);
        String lowerQuery = query.toLowerCase(Locale.ROOT);
        int index = lowerSummary.indexOf(lowerQuery);
        int start = Math.max(0, index < 0 ? 0 : index - 60);
        int end = Math.min(summary.length(), start + 220);
        return summary.substring(start, end);
    }

    private List<RagHitView> buildDefaultScopedHits(List<LiteratureChunk> chunks, Set<Long> scopedLiteratureIds, int limit) {
        if (scopedLiteratureIds.isEmpty()) {
            return List.of();
        }

        if (!chunks.isEmpty()) {
            return chunks.stream()
                    .filter(chunk -> scopedLiteratureIds.contains(chunk.getLiterature().getId()))
                    .sorted(Comparator.comparingInt(LiteratureChunk::getChunkIndex))
                    .limit(limit)
                    .map(chunk -> new RagHitView(
                            chunk.getLiterature().getId(),
                            chunk.getLiterature().getTitle(),
                            buildSnippet(chunk.getContent(), ""),
                            0.1,
                            chunk.getLiterature().getSourceType(),
                            chunk.getLiterature().getPublicationDate() == null ? "" : chunk.getLiterature().getPublicationDate().format(DateTimeFormatter.ISO_DATE),
                            chunk.getChunkLabel(),
                            "SCOPED_FALLBACK"))
                    .toList();
        }

        return literatureService.list(null).stream()
                .filter(view -> scopedLiteratureIds.contains(view.id()))
                .limit(limit)
                .map(view -> new RagHitView(
                        view.id(),
                        view.title(),
                        buildSnippet(view.summary(), ""),
                        0.1,
                        view.sourceType(),
                        view.publicationDate() == null ? "" : view.publicationDate().format(DateTimeFormatter.ISO_DATE),
                        "summary",
                        "SCOPED_FALLBACK"))
                .toList();
    }
}
