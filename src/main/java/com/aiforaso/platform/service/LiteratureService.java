package com.aiforaso.platform.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.aiforaso.platform.domain.Literature;
import com.aiforaso.platform.dto.LiteratureCreateRequest;
import com.aiforaso.platform.dto.LiteratureImportRequest;
import com.aiforaso.platform.dto.LiteratureView;
import com.aiforaso.platform.repository.LiteratureRepository;
import com.aiforaso.platform.security.AuthContext;

@Service
public class LiteratureService {

    private final LiteratureRepository literatureRepository;
    private final DocumentParserService documentParserService;
    private final LiteratureInsightService literatureInsightService;

    public LiteratureService(
            LiteratureRepository literatureRepository,
            DocumentParserService documentParserService,
            LiteratureInsightService literatureInsightService) {
        this.literatureRepository = literatureRepository;
        this.documentParserService = documentParserService;
        this.literatureInsightService = literatureInsightService;
    }

    @Transactional
    public LiteratureView create(LiteratureCreateRequest request) {
        Literature literature = new Literature();
        literature.setTitle(request.title());
        literature.setSourceType(request.sourceType());
        literature.setDiseaseArea(request.diseaseArea());
        literature.setSummary(request.summary());
        literature.setKeywords(request.keywords());
        literature.setPublicationDate(request.publicationDate());
        literature.setStoragePath(request.storagePath());
        literature.setCreatedBy(AuthContext.getCurrentUserId());
        return toView(literatureRepository.save(literature));
    }

    @Transactional
    public LiteratureView importFromFile(LiteratureImportRequest request) {
        Path resolvedPath = resolveStoragePath(request.storagePath());
        if (!Files.exists(resolvedPath) || Files.isDirectory(resolvedPath)) {
            throw new IllegalArgumentException("Import file not found: " + request.storagePath());
        }

        String sourceType = StringUtils.hasText(request.sourceType()) ? request.sourceType() : detectSourceType(resolvedPath);
        String title = StringUtils.hasText(request.title()) ? request.title() : stripExtension(resolvedPath.getFileName().toString());
        String diseaseArea = StringUtils.hasText(request.diseaseArea()) ? request.diseaseArea() : "general drug research";
        String summary = buildImportedSummary(resolvedPath, title, sourceType, diseaseArea, request.keywords());

        return create(new LiteratureCreateRequest(
                title,
                sourceType,
                diseaseArea,
                summary,
                request.keywords(),
                request.publicationDate(),
                resolvedPath.toString()));
    }

    @Transactional
    public LiteratureView importUploadedFile(
            MultipartFile file,
            String title,
            String sourceType,
            String diseaseArea,
            String keywords,
            java.time.LocalDate publicationDate) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file is empty");
        }

        try {
            String originalFilename = StringUtils.hasText(file.getOriginalFilename())
                    ? file.getOriginalFilename()
                    : "uploaded-file";
            String sanitizedFilename = sanitizeFileName(originalFilename);
            Path uploadDir = Path.of("uploads", "literatures").toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);

            String storedFilename = UUID.randomUUID() + "_" + sanitizedFilename;
            Path targetPath = uploadDir.resolve(storedFilename).normalize();
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            return importFromFile(new LiteratureImportRequest(
                    targetPath.toString(),
                    title,
                    sourceType,
                    diseaseArea,
                    keywords,
                    publicationDate));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to store uploaded file", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<LiteratureView> list(String keyword) {
        Long currentUserId = AuthContext.getCurrentUserId();
        List<Literature> literatures;
        if (currentUserId != null) {
            literatures = StringUtils.hasText(keyword)
                    ? literatureRepository.findByCreatedByAndTitleContainingIgnoreCaseOrCreatedByAndKeywordsContainingIgnoreCase(
                            currentUserId, keyword, currentUserId, keyword)
                    : literatureRepository.findByCreatedBy(currentUserId);
        } else {
            literatures = StringUtils.hasText(keyword)
                    ? literatureRepository.findByTitleContainingIgnoreCaseOrKeywordsContainingIgnoreCase(keyword, keyword)
                    : literatureRepository.findAll();
        }
        return literatures.stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public Literature getEntity(Long id) {
        return literatureRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Literature not found: " + id));
    }

    @Transactional
    public Literature saveEntity(Literature literature) {
        return literatureRepository.save(literature);
    }

    @Transactional(readOnly = true)
    public LiteratureView get(Long id) {
        return toView(getEntity(id));
    }

    public LiteratureView toView(Literature literature) {
        return new LiteratureView(
                literature.getId(),
                literature.getTitle(),
                literature.getSourceType(),
                literature.getDiseaseArea(),
                literature.getSummary(),
                literature.getKeywords(),
                literature.getPublicationDate(),
                literature.getStoragePath(),
                literature.getVectorSyncStatus(),
                literature.getVectorSyncDetail(),
                literature.getVectorSyncedChunkCount(),
                literature.getVectorSyncedAt(),
                literature.getCreatedAt(),
                literature.getCreatedBy());
    }

    public Path resolveStoragePath(String rawPath) {
        Path directPath = Path.of(rawPath).normalize();
        if (directPath.isAbsolute() && Files.exists(directPath)) {
            return directPath;
        }

        Path currentDir = Path.of("").toAbsolutePath().normalize();
        Path candidate = currentDir.resolve(rawPath).normalize();
        if (Files.exists(candidate)) {
            return candidate;
        }

        Path parentCandidate = currentDir.getParent() == null
                ? candidate
                : currentDir.getParent().resolve(rawPath).normalize();
        return parentCandidate;
    }

    private String detectSourceType(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".pdf")) {
            return "PDF";
        }
        if (name.endsWith(".xlsx") || name.endsWith(".xls")) {
            return "EXCEL";
        }
        if (name.endsWith(".doc") || name.endsWith(".docx")) {
            return "DOC";
        }
        if (name.endsWith(".ppt") || name.endsWith(".pptx")) {
            return "PPT";
        }
        if (name.endsWith(".md") || name.endsWith(".txt")) {
            return "TEXT";
        }
        return "FILE";
    }

    private String stripExtension(String fileName) {
        int index = fileName.lastIndexOf('.');
        return index > 0 ? fileName.substring(0, index) : fileName;
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replace("\\", "_")
                .replace("/", "_")
                .replace(":", "_")
                .replace("*", "_")
                .replace("?", "_")
                .replace("\"", "_")
                .replace("<", "_")
                .replace(">", "_")
                .replace("|", "_");
    }

    private String buildImportedSummary(Path path, String title, String sourceType, String diseaseArea, String keywords) {
        try {
            String extractedText = documentParserService.extractText(path, sourceType);
            if (StringUtils.hasText(extractedText)) {
                var insight = literatureInsightService.analyze(title, diseaseArea, keywords, extractedText, List.of());
                if (StringUtils.hasText(insight.overviewSummary())) {
                    return truncate(insight.overviewSummary());
                }
            }

            long size = Files.size(path);
            return truncate("Imported " + sourceType + " file `" + path.getFileName() + "` for " + diseaseArea
                    + ". File size=" + size + " bytes. The system has stored the file and will continue parsing, extraction, and graph preparation in the next step.");
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to read import file: " + path, exception);
        }
    }

    private String truncate(String value) {
        return value.length() <= 4000 ? value : value.substring(0, 4000);
    }
}
