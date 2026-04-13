package com.aiforaso.platform.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.aiforaso.platform.domain.LiteratureChunk;

public interface LiteratureChunkRepository extends JpaRepository<LiteratureChunk, Long> {

    List<LiteratureChunk> findByLiteratureIdOrderByChunkIndexAsc(Long literatureId);

    void deleteByLiteratureId(Long literatureId);

    @Query("SELECT c FROM LiteratureChunk c JOIN FETCH c.literature")
    List<LiteratureChunk> findAllWithLiterature();

    @Query("SELECT c FROM LiteratureChunk c JOIN FETCH c.literature WHERE c.id IN :ids")
    List<LiteratureChunk> findAllByIdWithLiterature(@Param("ids") List<Long> ids);
}
