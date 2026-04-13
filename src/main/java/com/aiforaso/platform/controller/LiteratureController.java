package com.aiforaso.platform.controller;

import java.util.List;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aiforaso.platform.dto.LiteratureCreateRequest;
import com.aiforaso.platform.dto.LiteratureChunkView;
import com.aiforaso.platform.dto.LiteratureImportRequest;
import com.aiforaso.platform.dto.LiteratureOverviewView;
import com.aiforaso.platform.dto.LiteratureVectorStatusView;
import com.aiforaso.platform.dto.LiteratureView;
import com.aiforaso.platform.dto.BatchLiteratureImportRequest;
import com.aiforaso.platform.dto.RagHitView;
import com.aiforaso.platform.service.LiteratureIngestionService;
import com.aiforaso.platform.service.LiteratureDeletionService;
import com.aiforaso.platform.service.LiteratureOverviewService;
import com.aiforaso.platform.service.LiteraturePreparationService;
import com.aiforaso.platform.service.LiteratureService;
import com.aiforaso.platform.service.OperationLogService;
import com.aiforaso.platform.service.RagService;
import com.aiforaso.platform.dto.RagQueryRequest;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/literatures")
public class LiteratureController {

    private final LiteratureService literatureService;
    private final LiteratureIngestionService literatureIngestionService;
    private final LiteratureDeletionService literatureDeletionService;
    private final LiteratureOverviewService literatureOverviewService;
    private final LiteraturePreparationService literaturePreparationService;
    private final RagService ragService;
    private final OperationLogService operationLogService;

    public LiteratureController(
            LiteratureService literatureService,
            LiteratureIngestionService literatureIngestionService,
            LiteratureDeletionService literatureDeletionService,
            LiteratureOverviewService literatureOverviewService,
            LiteraturePreparationService literaturePreparationService,
            RagService ragService,
            OperationLogService operationLogService) {
        this.literatureService = literatureService;
        this.literatureIngestionService = literatureIngestionService;
        this.literatureDeletionService = literatureDeletionService;
        this.literatureOverviewService = literatureOverviewService;
        this.literaturePreparationService = literaturePreparationService;
        this.ragService = ragService;
        this.operationLogService = operationLogService;
    }

    @GetMapping
    public List<LiteratureView> list(@RequestParam(required = false) String keyword) {
        return literatureService.list(keyword);
    }

    @GetMapping("/{id}")
    public LiteratureView get(@PathVariable Long id) {
        return literatureService.get(id);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        LiteratureView literature = literatureService.get(id);
        literatureDeletionService.deleteLiterature(id);
        operationLogService.record("system", "DELETE", "LITERATURE", String.valueOf(id), "Deleted literature: " + literature.title());
    }

    @GetMapping("/{id}/overview")
    public LiteratureOverviewView getOverview(@PathVariable Long id) {
        return literatureOverviewService.build(id);
    }

    @GetMapping("/search")
    public List<RagHitView> semanticSearch(@RequestParam String keyword,
                                           @RequestParam(required = false) Integer topK,
                                           @RequestParam(required = false) Long literatureId) {
        return ragService.retrieve(new RagQueryRequest(
                keyword,
                topK == null ? 5 : topK,
                literatureId == null ? null : List.of(literatureId)));
    }

    @PostMapping
    public LiteratureView create(@Valid @RequestBody LiteratureCreateRequest request) {
        LiteratureView literature = literatureService.create(request);
        operationLogService.record("system", "CREATE", "LITERATURE", String.valueOf(literature.id()), "Created literature record");
        return literature;
    }

    @PostMapping("/import")
    public LiteratureView importFromFile(@Valid @RequestBody LiteratureImportRequest request) {
        LiteratureView literature = literatureService.importFromFile(request);
        literaturePreparationService.prepareImportedLiterature(literature.id());
        LiteratureView refreshed = literatureService.get(literature.id());
        operationLogService.record("system", "IMPORT", "LITERATURE", String.valueOf(literature.id()), "Imported literature from file");
        return refreshed;
    }

    @PostMapping("/upload")
    public LiteratureView uploadAndImport(@RequestParam("file") MultipartFile file,
                                          @RequestParam(required = false, defaultValue = "false") boolean autoIngest,
                                          @RequestParam(required = false) String title,
                                          @RequestParam(required = false) String sourceType,
                                          @RequestParam(required = false) String diseaseArea,
                                          @RequestParam(required = false) String keywords,
                                          @RequestParam(required = false) java.time.LocalDate publicationDate) {
        LiteratureView literature = literatureService.importUploadedFile(
                file,
                title,
                sourceType,
                diseaseArea,
                keywords,
                publicationDate);
        if (autoIngest) {
            literaturePreparationService.prepareImportedLiterature(literature.id());
        }
        LiteratureView refreshed = literatureService.get(literature.id());
        operationLogService.record("system", "UPLOAD_IMPORT", "LITERATURE", String.valueOf(literature.id()), "Uploaded and imported literature");
        return refreshed;
    }

    @PostMapping("/batch-import")
    public List<LiteratureView> batchImport(@Valid @RequestBody BatchLiteratureImportRequest request) {
        List<LiteratureView> imported = request.items().stream()
                .map(literatureService::importFromFile)
                .peek(view -> {
                    if (request.autoIngest()) {
                        literaturePreparationService.prepareImportedLiterature(view.id());
                    }
                    operationLogService.record("system", "BATCH_IMPORT", "LITERATURE", String.valueOf(view.id()), "Batch imported literature");
                })
                .map(view -> literatureService.get(view.id()))
                .toList();
        return imported;
    }

    @PostMapping("/{literatureId}/ingest")
    public List<LiteratureChunkView> ingest(@PathVariable Long literatureId) {
        List<LiteratureChunkView> chunks = literatureIngestionService.ingest(literatureId);
        operationLogService.record("system", "INGEST", "LITERATURE", String.valueOf(literatureId), "Generated literature chunks");
        return chunks;
    }

    @PostMapping("/{literatureId}/vectorize")
    public List<LiteratureChunkView> vectorize(@PathVariable Long literatureId) {
        List<LiteratureChunkView> chunks = literatureIngestionService.vectorize(literatureId);
        operationLogService.record("system", "VECTORIZE", "LITERATURE", String.valueOf(literatureId), "Vectorized literature chunks");
        return chunks;
    }

    @GetMapping("/{literatureId}/chunks")
    public List<LiteratureChunkView> listChunks(@PathVariable Long literatureId) {
        return literatureIngestionService.list(literatureId);
    }

    @GetMapping("/{literatureId}/vector-status")
    public LiteratureVectorStatusView vectorStatus(@PathVariable Long literatureId) {
        return literatureIngestionService.vectorStatus(literatureId);
    }
}
