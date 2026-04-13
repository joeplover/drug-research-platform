package com.aiforaso.platform.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiforaso.platform.domain.OperationLog;
import com.aiforaso.platform.dto.OperationLogView;
import com.aiforaso.platform.repository.OperationLogRepository;

@Service
public class OperationLogService {

    private final OperationLogRepository operationLogRepository;

    public OperationLogService(OperationLogRepository operationLogRepository) {
        this.operationLogRepository = operationLogRepository;
    }

    @Transactional
    public void record(String operatorEmail, String actionType, String resourceType, String resourceId, String detail) {
        OperationLog log = new OperationLog();
        log.setOperatorEmail(operatorEmail);
        log.setActionType(actionType);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setDetail(detail);
        operationLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<OperationLogView> list(String operatorEmail) {
        List<OperationLog> logs = (operatorEmail == null || operatorEmail.isBlank())
                ? operationLogRepository.findTop100ByOrderByCreatedAtDesc()
                : operationLogRepository.findTop100ByOperatorEmailOrderByCreatedAtDesc(operatorEmail);
        return logs.stream().map(log -> new OperationLogView(
                log.getId(),
                log.getOperatorEmail(),
                log.getActionType(),
                log.getResourceType(),
                log.getResourceId(),
                log.getDetail(),
                log.getCreatedAt())).toList();
    }
}
