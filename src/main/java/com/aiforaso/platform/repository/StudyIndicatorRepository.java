package com.aiforaso.platform.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.aiforaso.platform.domain.StudyIndicator;

public interface StudyIndicatorRepository extends JpaRepository<StudyIndicator, Long> {

    List<StudyIndicator> findByLiteratureId(Long literatureId);

    Page<StudyIndicator> findByLiteratureId(Long literatureId, Pageable pageable);

    List<StudyIndicator> findByLiteratureIdAndReviewStatusIgnoreCase(Long literatureId, String reviewStatus);

    long countByReviewStatusIgnoreCase(String reviewStatus);

    void deleteByLiteratureId(Long literatureId);
}
