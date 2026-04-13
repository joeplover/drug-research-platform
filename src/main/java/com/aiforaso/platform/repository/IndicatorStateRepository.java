package com.aiforaso.platform.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiforaso.platform.domain.IndicatorState;

public interface IndicatorStateRepository extends JpaRepository<IndicatorState, Long> {

    List<IndicatorState> findByLiteratureIdOrderByIndicatorNameAscStateOrderAsc(Long literatureId);

    void deleteByLiteratureId(Long literatureId);
}
