package com.aiforaso.platform.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aiforaso.platform.dto.OperationLogView;
import com.aiforaso.platform.service.OperationLogService;

@RestController
@RequestMapping("/api/logs")
public class OperationLogController {

    private final OperationLogService operationLogService;

    public OperationLogController(OperationLogService operationLogService) {
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public List<OperationLogView> list(@RequestParam(required = false) String operatorEmail) {
        return operationLogService.list(operatorEmail);
    }
}
