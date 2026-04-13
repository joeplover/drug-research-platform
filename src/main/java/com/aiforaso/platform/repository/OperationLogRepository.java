package com.aiforaso.platform.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiforaso.platform.domain.OperationLog;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    List<OperationLog> findTop100ByOrderByCreatedAtDesc();

    List<OperationLog> findTop100ByOperatorEmailOrderByCreatedAtDesc(String operatorEmail);
}
