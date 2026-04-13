package com.aiforaso.platform.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiforaso.platform.dto.AnalysisTaskDetailView;
import com.aiforaso.platform.dto.AnalysisTaskView;
import com.aiforaso.platform.service.AnalysisTaskQueryService;

@RestController
@RequestMapping("/api/tasks")
public class AnalysisTaskController {

    private final AnalysisTaskQueryService analysisTaskQueryService;

    public AnalysisTaskController(AnalysisTaskQueryService analysisTaskQueryService) {
        this.analysisTaskQueryService = analysisTaskQueryService;
    }

    @GetMapping
    public List<AnalysisTaskView> list() {
        return analysisTaskQueryService.list();
    }

    @GetMapping("/{id}")
    public AnalysisTaskView get(@PathVariable Long id) {
        return analysisTaskQueryService.get(id);
    }

    @GetMapping("/{id}/detail")
    public AnalysisTaskDetailView getDetail(@PathVariable Long id) {
        return analysisTaskQueryService.getDetail(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        analysisTaskQueryService.delete(id);
    }
}
