package com.aiforaso.platform.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiforaso.platform.dto.AnalysisReportRequest;
import com.aiforaso.platform.dto.AnalysisReportResponse;
import com.aiforaso.platform.dto.TopologyBuildRequest;
import com.aiforaso.platform.dto.TopologyBuildResponse;
import com.aiforaso.platform.service.AnalysisService;
import com.aiforaso.platform.service.OperationLogService;
import com.aiforaso.platform.service.TopologyAnalysisService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;
    private final TopologyAnalysisService topologyAnalysisService;
    private final OperationLogService operationLogService;

    public AnalysisController(
            AnalysisService analysisService,
            TopologyAnalysisService topologyAnalysisService,
            OperationLogService operationLogService) {
        this.analysisService = analysisService;
        this.topologyAnalysisService = topologyAnalysisService;
        this.operationLogService = operationLogService;
    }

    @PostMapping("/reports")
    public AnalysisReportResponse generateReport(@Valid @RequestBody AnalysisReportRequest request) {
        AnalysisReportResponse response = analysisService.generate(request);
        operationLogService.record("system", "ANALYZE", "REPORT", String.valueOf(response.taskId()), "Generated analysis report");
        return response;
    }

    @PostMapping("/literatures/{literatureId}/topology")
    public TopologyBuildResponse buildTopology(
            @PathVariable Long literatureId,
            @RequestBody(required = false) TopologyBuildRequest request) {
        TopologyBuildRequest safeRequest = request == null
                ? new TopologyBuildRequest(null, false)
                : request;
        TopologyBuildResponse response = topologyAnalysisService.build(literatureId, safeRequest);
        operationLogService.record("system", "BUILD_TOPOLOGY", "LITERATURE", String.valueOf(literatureId), "Built indicator topology");
        return response;
    }

    @GetMapping("/literatures/{literatureId}/topology")
    public TopologyBuildResponse getTopology(@PathVariable Long literatureId) {
        return topologyAnalysisService.getExisting(literatureId);
    }
}
