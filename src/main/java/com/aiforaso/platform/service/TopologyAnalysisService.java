package com.aiforaso.platform.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiforaso.platform.domain.AnalysisTask;
import com.aiforaso.platform.domain.IndicatorState;
import com.aiforaso.platform.domain.StateTransition;
import com.aiforaso.platform.dto.IndicatorStateView;
import com.aiforaso.platform.dto.IndicatorView;
import com.aiforaso.platform.dto.KnowledgeGraphResponse;
import com.aiforaso.platform.dto.StateTransitionView;
import com.aiforaso.platform.dto.TopologyBuildRequest;
import com.aiforaso.platform.dto.TopologyBuildResponse;
import com.aiforaso.platform.repository.AnalysisTaskRepository;
import com.aiforaso.platform.repository.IndicatorStateRepository;
import com.aiforaso.platform.repository.StateTransitionRepository;

@Service
public class TopologyAnalysisService {

    private final LiteratureService literatureService;
    private final KnowledgeExtractionService knowledgeExtractionService;
    private final IndicatorStateRepository indicatorStateRepository;
    private final StateTransitionRepository stateTransitionRepository;
    private final AnalysisTaskRepository analysisTaskRepository;

    private record IndicatorKey(String indicatorName, String timeWindow, String cohort) {
        static IndicatorKey from(IndicatorView v) {
            return new IndicatorKey(
                v.indicatorName(),
                v.timeWindow() == null ? "unknown" : v.timeWindow(),
                v.cohort() == null ? "overall" : v.cohort()
            );
        }
    }

    public TopologyAnalysisService(
            LiteratureService literatureService,
            KnowledgeExtractionService knowledgeExtractionService,
            IndicatorStateRepository indicatorStateRepository,
            StateTransitionRepository stateTransitionRepository,
            AnalysisTaskRepository analysisTaskRepository) {
        this.literatureService = literatureService;
        this.knowledgeExtractionService = knowledgeExtractionService;
        this.indicatorStateRepository = indicatorStateRepository;
        this.stateTransitionRepository = stateTransitionRepository;
        this.analysisTaskRepository = analysisTaskRepository;
    }

    @Transactional
    public TopologyBuildResponse build(Long literatureId, TopologyBuildRequest request) {
        var literature = literatureService.getEntity(literatureId);
        
        List<IndicatorView> indicators;
        try {
            indicators = request.rebuildIndicators()
                    ? knowledgeExtractionService.extractIndicatorsByChunks(literatureId)
                    : ensureIndicators(literatureId);
            System.out.println("[DEBUG] literatureId=" + literatureId + ", indicators count=" + indicators.size());
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("指标抽取失败，请检查文献是否有内容: " + e.getMessage());
        }

        String targetIndicator = request.indicatorName();
        List<IndicatorView> filtered = indicators.stream()
                .filter(indicator -> targetIndicator == null || targetIndicator.isBlank()
                        || indicator.indicatorName().toLowerCase().contains(targetIndicator.toLowerCase()))
                .sorted(Comparator.comparing(IndicatorView::indicatorName).thenComparing(IndicatorView::timeWindow, Comparator.nullsLast(String::compareTo)))
                .toList();

        System.out.println("[DEBUG] targetIndicator=" + targetIndicator + ", filtered count=" + filtered.size());

        if (filtered.isEmpty()) {
            throw new IllegalArgumentException("文献 [" + literature.getTitle() + "] 没有可用的指标数据。请先在【文献指标抽取】页面运行抽取。");
        }

        stateTransitionRepository.deleteByLiteratureId(literatureId);
        indicatorStateRepository.deleteByLiteratureId(literatureId);

        Map<IndicatorKey, List<IndicatorView>> grouped = filtered.stream()
                .collect(Collectors.groupingBy(IndicatorKey::from));

        List<IndicatorState> states = new ArrayList<>();
        List<StateTransition> transitions = new ArrayList<>();
        
        for (Map.Entry<IndicatorKey, List<IndicatorView>> entry : grouped.entrySet()) {
            IndicatorKey key = entry.getKey();
            List<IndicatorView> groupIndicators = entry.getValue();
            
            IndicatorView representative = groupIndicators.get(0);
            String mergedEvidenceLocators = groupIndicators.stream()
                    .map(IndicatorView::evidenceLocator)
                    .distinct()
                    .collect(Collectors.joining(" | "));

            String stateLabelPrefix = key.indicatorName() + " [" + key.timeWindow() + "/" + key.cohort() + "]";
            
            IndicatorState initialState = saveState(literatureId, representative, 0, "INITIAL",
                    stateLabelPrefix + " 基线",
                    "研究开始前 " + key.indicatorName() + " 的基线预期状态",
                    mergedEvidenceLocators);
            IndicatorState observedState = saveState(literatureId, representative, 1, "OBSERVED",
                    stateLabelPrefix + " 观测",
                    buildObservedDescription(representative),
                    mergedEvidenceLocators);
            IndicatorState finalState = saveState(literatureId, representative, 2, "FINAL",
                    stateLabelPrefix + " 结论",
                    buildFinalDescription(representative),
                    mergedEvidenceLocators);

            states.add(initialState);
            states.add(observedState);
            states.add(finalState);

            BigDecimal baseProbability = representative.confidenceScore() != null 
                    ? representative.confidenceScore() 
                    : new BigDecimal("0.75");
            BigDecimal initialToObservedProb = baseProbability.multiply(new BigDecimal("0.96")).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal observedToFinalProb = baseProbability.multiply(new BigDecimal("0.91")).setScale(2, java.math.RoundingMode.HALF_UP);

            transitions.add(saveTransition(literatureId, initialState, observedState,
                    "随访证据显示在 " + key.timeWindow() + " 时 " + key.cohort() + " 队列有可测量的变化",
                    initialToObservedProb,
                    mergedEvidenceLocators));
            transitions.add(saveTransition(literatureId, observedState, finalState,
                    "观测变化被解释为 " + inferDirection(representative.observedValue()) + " 的 " + representative.category() + " 响应",
                    observedToFinalProb,
                    mergedEvidenceLocators));
        }

        AnalysisTask task = new AnalysisTask();
        task.setTaskType("TOPOLOGY");
        task.setStatus("COMPLETED");
        task.setInputText(targetIndicator == null || targetIndicator.isBlank() ? "ALL_INDICATORS" : targetIndicator);
        task.setResultSummary("Built topology for " + grouped.size() + " unique indicator groups (from " + filtered.size() + " observations) in literature `" + literature.getTitle() + "`.");
        task.setCitations(filtered.stream().map(IndicatorView::evidenceLocator).distinct().collect(Collectors.joining(" | ")));
        task.setContextLiteratureIds(String.valueOf(literatureId));
        task = analysisTaskRepository.save(task);

        List<IndicatorStateView> stateViews = states.stream().map(this::toStateView).toList();
        List<StateTransitionView> transitionViews = transitions.stream().map(this::toTransitionView).toList();

        return new TopologyBuildResponse(
                task.getId(),
                literatureId,
                targetIndicator,
                stateViews,
                transitionViews,
                buildGraph(stateViews, transitionViews));
    }

    @Transactional(readOnly = true)
    public TopologyBuildResponse getExisting(Long literatureId) {
        List<IndicatorStateView> states = indicatorStateRepository.findByLiteratureIdOrderByIndicatorNameAscStateOrderAsc(literatureId)
                .stream().map(this::toStateView).toList();
        List<StateTransitionView> transitions = stateTransitionRepository.findByLiteratureIdOrderByIdAsc(literatureId)
                .stream().map(this::toTransitionView).toList();
        return new TopologyBuildResponse(null, literatureId, null, states, transitions, buildGraph(states, transitions));
    }

    private List<IndicatorView> ensureIndicators(Long literatureId) {
        List<IndicatorView> existing = knowledgeExtractionService.listByLiterature(literatureId);
        return existing.isEmpty() ? knowledgeExtractionService.extractIndicatorsByChunks(literatureId) : existing;
    }

    private IndicatorState saveState(
            Long literatureId,
            IndicatorView indicator,
            int order,
            String stageType,
            String label,
            String description,
            String evidenceLocator) {
        IndicatorState state = new IndicatorState();
        state.setLiterature(literatureService.getEntity(literatureId));
        state.setIndicatorName(indicator.indicatorName());
        state.setStageType(stageType);
        state.setStateOrder(order);
        state.setStateLabel(label);
        state.setDescription(description);
        state.setEvidenceLocator(evidenceLocator);
        return indicatorStateRepository.save(state);
    }

    private StateTransition saveTransition(
            Long literatureId,
            IndicatorState fromState,
            IndicatorState toState,
            String conditionText,
            BigDecimal probability,
            String evidenceLocator) {
        StateTransition transition = new StateTransition();
        transition.setLiterature(literatureService.getEntity(literatureId));
        transition.setFromState(fromState);
        transition.setToState(toState);
        transition.setConditionText(conditionText);
        transition.setTransitionProbability(probability);
        transition.setEvidenceLocator(evidenceLocator);
        return stateTransitionRepository.save(transition);
    }

    private String buildObservedDescription(IndicatorView indicator) {
        String value = indicator.observedValue() == null ? "未量化变化" : indicator.observedValue().toPlainString();
        String timeWindow = indicator.timeWindow() == null ? "未知时间窗口" : indicator.timeWindow();
        return "在 " + timeWindow + " 观测到 " + indicator.indicatorName() 
                + " 信号，值为 " + value + "，队列：" + safe(indicator.cohort());
    }

    private String buildFinalDescription(IndicatorView indicator) {
        String direction = inferDirection(indicator.observedValue());
        return "解释为 " + indicator.indicatorName() + " 的 " + direction + " " + indicator.category() + " 响应";
    }

    private String inferDirection(BigDecimal value) {
        if (value == null) {
            return "未量化";
        }
        int sign = value.compareTo(BigDecimal.ZERO);
        if (sign < 0) {
            return "下降";
        }
        if (sign > 0) {
            return "上升";
        }
        return "稳定";
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "overall" : value.toLowerCase(Locale.ROOT);
    }

    private IndicatorStateView toStateView(IndicatorState state) {
        return new IndicatorStateView(
                state.getId(),
                state.getLiterature().getId(),
                state.getIndicatorName(),
                state.getStageType(),
                state.getStateOrder(),
                state.getStateLabel(),
                state.getDescription(),
                state.getEvidenceLocator());
    }

    private StateTransitionView toTransitionView(StateTransition transition) {
        return new StateTransitionView(
                transition.getId(),
                transition.getLiterature().getId(),
                transition.getFromState().getId(),
                transition.getToState().getId(),
                transition.getConditionText(),
                transition.getTransitionProbability(),
                transition.getEvidenceLocator());
    }

    private KnowledgeGraphResponse buildGraph(List<IndicatorStateView> states, List<StateTransitionView> transitions) {
        List<KnowledgeGraphResponse.GraphNode> nodes = states.stream()
                .map(state -> new KnowledgeGraphResponse.GraphNode(
                        "state-" + state.id(),
                        state.stateLabel(),
                        "指标状态"))
                .toList();
        List<KnowledgeGraphResponse.GraphEdge> edges = transitions.stream()
                .map(transition -> new KnowledgeGraphResponse.GraphEdge(
                        "state-" + transition.fromStateId(),
                        "state-" + transition.toStateId(),
                        "转移"))
                .toList();
        return new KnowledgeGraphResponse(nodes, edges);
    }
}
