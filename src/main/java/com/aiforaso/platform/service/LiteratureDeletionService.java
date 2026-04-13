package com.aiforaso.platform.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.aiforaso.platform.domain.AnalysisTask;
import com.aiforaso.platform.domain.Literature;
import com.aiforaso.platform.repository.AnalysisTaskRepository;
import com.aiforaso.platform.repository.IndicatorStateRepository;
import com.aiforaso.platform.repository.LiteratureChunkRepository;
import com.aiforaso.platform.repository.LiteratureRepository;
import com.aiforaso.platform.repository.StateTransitionRepository;
import com.aiforaso.platform.repository.StudyIndicatorRepository;

@Service
public class LiteratureDeletionService {

    private final LiteratureRepository literatureRepository;
    private final LiteratureChunkRepository literatureChunkRepository;
    private final StudyIndicatorRepository studyIndicatorRepository;
    private final IndicatorStateRepository indicatorStateRepository;
    private final StateTransitionRepository stateTransitionRepository;
    private final AnalysisTaskRepository analysisTaskRepository;
    private final MilvusVectorStoreService milvusVectorStoreService;
    private final LiteratureService literatureService;

    public LiteratureDeletionService(
            LiteratureRepository literatureRepository,
            LiteratureChunkRepository literatureChunkRepository,
            StudyIndicatorRepository studyIndicatorRepository,
            IndicatorStateRepository indicatorStateRepository,
            StateTransitionRepository stateTransitionRepository,
            AnalysisTaskRepository analysisTaskRepository,
            MilvusVectorStoreService milvusVectorStoreService,
            LiteratureService literatureService) {
        this.literatureRepository = literatureRepository;
        this.literatureChunkRepository = literatureChunkRepository;
        this.studyIndicatorRepository = studyIndicatorRepository;
        this.indicatorStateRepository = indicatorStateRepository;
        this.stateTransitionRepository = stateTransitionRepository;
        this.analysisTaskRepository = analysisTaskRepository;
        this.milvusVectorStoreService = milvusVectorStoreService;
        this.literatureService = literatureService;
    }

    @Transactional
    public void deleteLiterature(Long literatureId) {
        Literature literature = literatureService.getEntity(literatureId);
        String storagePath = literature.getStoragePath();

        milvusVectorStoreService.deleteByLiteratureId(literatureId);
        stateTransitionRepository.deleteByLiteratureId(literatureId);
        indicatorStateRepository.deleteByLiteratureId(literatureId);
        studyIndicatorRepository.deleteByLiteratureId(literatureId);
        literatureChunkRepository.deleteByLiteratureId(literatureId);
        refreshTaskContexts(literatureId);
        literatureRepository.delete(literature);
        literatureRepository.flush();

        deleteManagedStorageFile(storagePath);
    }

    private void refreshTaskContexts(Long deletedLiteratureId) {
        for (AnalysisTask task : analysisTaskRepository.findAll()) {
            String currentContext = task.getContextLiteratureIds();
            if (currentContext == null || currentContext.isBlank()) {
                continue;
            }

            String updatedContext = Arrays.stream(currentContext.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .filter(item -> !item.equals(String.valueOf(deletedLiteratureId)))
                    .collect(Collectors.joining(","));

            if (!updatedContext.equals(currentContext)) {
                task.setContextLiteratureIds(updatedContext);
                analysisTaskRepository.save(task);
            }
        }
    }

    private void deleteManagedStorageFile(String rawStoragePath) {
        if (rawStoragePath == null || rawStoragePath.isBlank()) {
            return;
        }

        try {
            Path resolvedPath = literatureService.resolveStoragePath(rawStoragePath);
            Path managedUploadDir = Path.of("").toAbsolutePath().normalize()
                    .resolve("uploads")
                    .resolve("literatures")
                    .normalize();

            if (Files.exists(resolvedPath)
                    && !Files.isDirectory(resolvedPath)
                    && resolvedPath.normalize().startsWith(managedUploadDir)) {
                Files.deleteIfExists(resolvedPath);
            }
        } catch (Exception ignored) {
        }
    }
}
