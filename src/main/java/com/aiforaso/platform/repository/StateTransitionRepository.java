package com.aiforaso.platform.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiforaso.platform.domain.StateTransition;

public interface StateTransitionRepository extends JpaRepository<StateTransition, Long> {

    List<StateTransition> findByLiteratureIdOrderByIdAsc(Long literatureId);

    void deleteByLiteratureId(Long literatureId);
}
