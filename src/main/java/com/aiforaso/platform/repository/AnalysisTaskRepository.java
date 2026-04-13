package com.aiforaso.platform.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiforaso.platform.domain.AnalysisTask;

public interface AnalysisTaskRepository extends JpaRepository<AnalysisTask, Long> {
}
