package com.aiforaso.platform.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aiforaso.platform.domain.Literature;

public interface LiteratureRepository extends JpaRepository<Literature, Long> {

    List<Literature> findByTitleContainingIgnoreCaseOrKeywordsContainingIgnoreCase(String titleKeyword, String tagsKeyword);

    List<Literature> findByCreatedBy(Long createdBy);

    List<Literature> findByCreatedByAndTitleContainingIgnoreCaseOrCreatedByAndKeywordsContainingIgnoreCase(
            Long createdBy1, String titleKeyword, Long createdBy2, String tagsKeyword);
}
