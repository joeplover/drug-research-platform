package com.aiforaso.platform.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.aiforaso.platform.dto.AnalysisTaskView;
import com.aiforaso.platform.dto.KnowledgeGraphResponse;
import com.aiforaso.platform.dto.LiteratureView;
import com.aiforaso.platform.dto.UserView;
import com.aiforaso.platform.repository.IndicatorStateRepository;
import com.aiforaso.platform.repository.StateTransitionRepository;

@Service
public class KnowledgeGraphService {

    private final LiteratureService literatureService;
    private final KnowledgeExtractionService knowledgeExtractionService;
    private final UserService userService;
    private final AnalysisTaskQueryService analysisTaskQueryService;
    private final IndicatorStateRepository indicatorStateRepository;
    private final StateTransitionRepository stateTransitionRepository;

    public KnowledgeGraphService(
            LiteratureService literatureService,
            KnowledgeExtractionService knowledgeExtractionService,
            UserService userService,
            AnalysisTaskQueryService analysisTaskQueryService,
            IndicatorStateRepository indicatorStateRepository,
            StateTransitionRepository stateTransitionRepository) {
        this.literatureService = literatureService;
        this.knowledgeExtractionService = knowledgeExtractionService;
        this.userService = userService;
        this.analysisTaskQueryService = analysisTaskQueryService;
        this.indicatorStateRepository = indicatorStateRepository;
        this.stateTransitionRepository = stateTransitionRepository;
    }

    public KnowledgeGraphResponse buildGraph() {
        return buildGraphForLiterature(null);
    }

    public KnowledgeGraphResponse buildGraphForLiterature(Long literatureIdFilter) {
        List<KnowledgeGraphResponse.GraphNode> nodes = new ArrayList<>();
        List<KnowledgeGraphResponse.GraphEdge> edges = new ArrayList<>();

        for (LiteratureView literature : literatureService.list(null)) {
            if (literatureIdFilter != null && !literature.id().equals(literatureIdFilter)) {
                continue;
            }

            String diseaseLabel = normalizeLabel(literature.diseaseArea(), "general drug research");
            String sourceType = normalizeLabel(literature.sourceType(), "unknown");
            String literatureNodeId = "literature-" + literature.id();
            String diseaseNodeId = "disease-" + normalizeId(diseaseLabel);

            nodes.add(new KnowledgeGraphResponse.GraphNode(diseaseNodeId, diseaseLabel, "疾病领域"));
            nodes.add(new KnowledgeGraphResponse.GraphNode(literatureNodeId, literature.title(), "文献"));
            edges.add(new KnowledgeGraphResponse.GraphEdge(diseaseNodeId, literatureNodeId, "包含"));
            addNodeAndEdge(nodes, edges, literatureNodeId, "source-" + normalizeId(sourceType), sourceType, "来源类型", "来源");

            if (literature.keywords() != null && !literature.keywords().isBlank()) {
                for (String keyword : literature.keywords().split("[,，;；/|]")) {
                    String normalizedKeyword = normalizeLabel(keyword, null);
                    if (normalizedKeyword == null) {
                        continue;
                    }
                    addNodeAndEdge(
                            nodes,
                            edges,
                            literatureNodeId,
                            "keyword-" + normalizeId(normalizedKeyword),
                            normalizedKeyword,
                            "关键词",
                            "提及");
                }
            }

            knowledgeExtractionService.listByLiterature(literature.id()).forEach(indicator -> {
                String reviewStatus = normalizeLabel(indicator.reviewStatus(), "待复核");
                String indicatorNodeId = "indicator-" + indicator.id();
                nodes.add(new KnowledgeGraphResponse.GraphNode(
                        indicatorNodeId,
                        indicator.indicatorName() + " [" + reviewStatus + "]",
                        "指标-" + reviewStatus));
                edges.add(new KnowledgeGraphResponse.GraphEdge(literatureNodeId, indicatorNodeId, "报告"));

                String category = normalizeLabel(indicator.category(), null);
                if (category != null) {
                    addNodeAndEdge(
                            nodes,
                            edges,
                            indicatorNodeId,
                            "indicator-category-" + normalizeId(category),
                            category,
                            "指标类别",
                            "属于");
                }

                String cohort = normalizeLabel(indicator.cohort(), null);
                if (cohort != null && !"overall".equalsIgnoreCase(cohort)) {
                    addNodeAndEdge(
                            nodes,
                            edges,
                            indicatorNodeId,
                            "cohort-" + normalizeId(cohort),
                            cohort,
                            "队列",
                            "观测于");
                }

                String timeWindow = normalizeLabel(indicator.timeWindow(), null);
                if (timeWindow != null && !"unspecified".equalsIgnoreCase(timeWindow)) {
                    addNodeAndEdge(
                            nodes,
                            edges,
                            indicatorNodeId,
                            "time-" + normalizeId(timeWindow),
                            timeWindow,
                            "时间窗口",
                            "观测时间");
                }
            });

            indicatorStateRepository.findByLiteratureIdOrderByIndicatorNameAscStateOrderAsc(literature.id()).forEach(state -> {
                String stateNodeId = "state-" + state.getId();
                nodes.add(new KnowledgeGraphResponse.GraphNode(stateNodeId, state.getStateLabel(), "指标状态"));
                edges.add(new KnowledgeGraphResponse.GraphEdge(literatureNodeId, stateNodeId, "包含状态"));
            });

            stateTransitionRepository.findByLiteratureIdOrderByIdAsc(literature.id()).forEach(transition -> {
                edges.add(new KnowledgeGraphResponse.GraphEdge(
                        "state-" + transition.getFromState().getId(),
                        "state-" + transition.getToState().getId(),
                        "转移"));
            });
        }

        if (literatureIdFilter == null) {
            for (UserView user : userService.list()) {
                nodes.add(new KnowledgeGraphResponse.GraphNode("user-" + user.id(), user.username(), "用户"));
            }
        }

        for (AnalysisTaskView task : analysisTaskQueryService.list()) {
            if (literatureIdFilter != null && !containsLiteratureId(task.contextLiteratureIds(), literatureIdFilter)) {
                continue;
            }

            String taskNodeId = "task-" + task.id();
            nodes.add(new KnowledgeGraphResponse.GraphNode(taskNodeId, task.taskType(), "任务"));
            if (task.contextLiteratureIds() != null && !task.contextLiteratureIds().isBlank()) {
                for (String literatureId : task.contextLiteratureIds().split(",")) {
                    String normalizedLiteratureId = normalizeLabel(literatureId, null);
                    if (normalizedLiteratureId != null) {
                        edges.add(new KnowledgeGraphResponse.GraphEdge(taskNodeId, "literature-" + normalizedLiteratureId, "分析"));
                    }
                }
            }
        }

        return new KnowledgeGraphResponse(
                nodes.stream().distinct().collect(Collectors.toList()),
                edges.stream().distinct().collect(Collectors.toList()));
    }

    public KnowledgeGraphResponse queryGraph(Long literatureId, String keyword, String nodeType, String reviewStatus) {
        KnowledgeGraphResponse fullGraph = literatureId == null ? buildGraph() : buildGraphForLiterature(literatureId);
        String normalizedKeyword = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT);
        String normalizedType = nodeType == null ? "" : nodeType.toLowerCase(Locale.ROOT);
        String normalizedReviewStatus = reviewStatus == null ? "" : reviewStatus.toLowerCase(Locale.ROOT);
        boolean hasFilter = !normalizedKeyword.isBlank() || !normalizedType.isBlank() || !normalizedReviewStatus.isBlank();

        List<KnowledgeGraphResponse.GraphNode> seedNodes = fullGraph.nodes().stream()
                .filter(node -> normalizedKeyword.isBlank() || node.label().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                .filter(node -> normalizedType.isBlank() || node.type().toLowerCase(Locale.ROOT).equals(normalizedType))
                .filter(node -> normalizedReviewStatus.isBlank()
                        || !node.type().startsWith("指标-")
                        || node.type().equals("指标-" + reviewStatus))
                .toList();

        Set<String> nodeIds = seedNodes.stream().map(KnowledgeGraphResponse.GraphNode::id).collect(Collectors.toSet());
        if (nodeIds.isEmpty() && hasFilter) {
            return new KnowledgeGraphResponse(List.of(), List.of());
        }
        List<KnowledgeGraphResponse.GraphEdge> filteredEdges = fullGraph.edges().stream()
                .filter(edge -> nodeIds.contains(edge.source()) || nodeIds.contains(edge.target()))
                .toList();

        Set<String> expandedNodeIds = new LinkedHashSet<>(nodeIds);
        filteredEdges.forEach(edge -> {
            expandedNodeIds.add(edge.source());
            expandedNodeIds.add(edge.target());
        });

        List<KnowledgeGraphResponse.GraphNode> filteredNodes = fullGraph.nodes().stream()
                .filter(node -> expandedNodeIds.isEmpty() || expandedNodeIds.contains(node.id()))
                .toList();

        return new KnowledgeGraphResponse(filteredNodes, filteredEdges);
    }

    private void addNodeAndEdge(
            List<KnowledgeGraphResponse.GraphNode> nodes,
            List<KnowledgeGraphResponse.GraphEdge> edges,
            String sourceId,
            String targetId,
            String targetLabel,
            String targetType,
            String relation) {
        nodes.add(new KnowledgeGraphResponse.GraphNode(targetId, targetLabel, targetType));
        edges.add(new KnowledgeGraphResponse.GraphEdge(sourceId, targetId, relation));
    }

    private String normalizeId(Object rawValue) {
        return String.valueOf(rawValue)
                .trim()
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "_")
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeLabel(String rawValue, String fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        return rawValue.trim();
    }

    private boolean containsLiteratureId(String contextLiteratureIds, Long literatureId) {
        if (contextLiteratureIds == null || contextLiteratureIds.isBlank() || literatureId == null) {
            return false;
        }
        Set<String> ids = new LinkedHashSet<>();
        for (String item : contextLiteratureIds.split(",")) {
            String normalized = normalizeLabel(item, null);
            if (normalized != null) {
                ids.add(normalized);
            }
        }
        return ids.contains(String.valueOf(literatureId));
    }
}
