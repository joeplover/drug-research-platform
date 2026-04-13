package com.aiforaso.platform.service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.aiforaso.platform.dto.AnalysisTaskDetailView;
import com.aiforaso.platform.dto.AnalysisTaskView;
import com.aiforaso.platform.dto.IndicatorView;
import com.aiforaso.platform.dto.RagHitView;
import com.aiforaso.platform.repository.AnalysisTaskRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AnalysisTaskQueryService {

    private final AnalysisTaskRepository analysisTaskRepository;
    private final ObjectMapper objectMapper;

    public AnalysisTaskQueryService(AnalysisTaskRepository analysisTaskRepository, ObjectMapper objectMapper) {
        this.analysisTaskRepository = analysisTaskRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AnalysisTaskView> list() {
        return analysisTaskRepository.findAll().stream()
                .sorted(Comparator.comparing(task -> task.getCreatedAt(), Comparator.nullsLast(Comparator.reverseOrder())))
                .map(task -> new AnalysisTaskView(
                        task.getId(),
                        task.getTaskType(),
                        task.getStatus(),
                        task.getInputText(),
                        task.getResultSummary(),
                        task.getCitations(),
                        task.getContextLiteratureIds(),
                        task.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AnalysisTaskView get(Long id) {
        var task = analysisTaskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Analysis task not found: " + id));
        return new AnalysisTaskView(
                task.getId(),
                task.getTaskType(),
                task.getStatus(),
                task.getInputText(),
                task.getResultSummary(),
                task.getCitations(),
                task.getContextLiteratureIds(),
                task.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public AnalysisTaskDetailView getDetail(Long id) {
        var task = analysisTaskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Analysis task not found: " + id));
        return new AnalysisTaskDetailView(
                task.getId(),
                task.getTaskType(),
                task.getStatus(),
                task.getInputText(),
                task.getResultSummary(),
                task.getCitations(),
                task.getContextLiteratureIds(),
                task.getAnalysisFocus(),
                readList(task.getEvidenceJson(), new TypeReference<List<RagHitView>>() {
                }),
                readList(task.getIndicatorsJson(), new TypeReference<List<IndicatorView>>() {
                }),
                task.getCreatedAt());
    }

    @Transactional
    public void delete(Long id) {
        if (!analysisTaskRepository.existsById(id)) {
            throw new IllegalArgumentException("Analysis task not found: " + id);
        }
        analysisTaskRepository.deleteById(id);
    }

    private <T> List<T> readList(String json, TypeReference<List<T>> typeReference) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception exception) {
            return List.of();
        }
    }
}
