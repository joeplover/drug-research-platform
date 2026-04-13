package com.aiforaso.platform.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiforaso.platform.dto.WorkflowProcessRequest;
import com.aiforaso.platform.dto.WorkflowProcessResponse;
import com.aiforaso.platform.service.OperationLogService;
import com.aiforaso.platform.service.WorkflowService;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final OperationLogService operationLogService;

    public WorkflowController(WorkflowService workflowService, OperationLogService operationLogService) {
        this.workflowService = workflowService;
        this.operationLogService = operationLogService;
    }

    @PostMapping("/literatures/{literatureId}/process")
    public WorkflowProcessResponse processLiterature(
            @PathVariable Long literatureId,
            @RequestBody(required = false) WorkflowProcessRequest request) {
        WorkflowProcessRequest safeRequest = request == null
                ? new WorkflowProcessRequest(null, null, false, false, false)
                : request;
        WorkflowProcessResponse response = workflowService.processLiterature(literatureId, safeRequest);
        operationLogService.record("system", "WORKFLOW", "LITERATURE", String.valueOf(literatureId), "Processed full literature workflow");
        return response;
    }
}
