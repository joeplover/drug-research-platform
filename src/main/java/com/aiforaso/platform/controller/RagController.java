package com.aiforaso.platform.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiforaso.platform.dto.RagHitView;
import com.aiforaso.platform.dto.RagQueryPageRequest;
import com.aiforaso.platform.dto.RagQueryRequest;
import com.aiforaso.platform.service.RagService;

import jakarta.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/query")
    public List<RagHitView> retrieve(@Valid @RequestBody RagQueryRequest request) {
        return ragService.retrieve(request);
    }

    @PostMapping("/query/page")
    public Page<RagHitView> retrievePaginated(@Valid @RequestBody RagQueryPageRequest request) {
        return ragService.retrievePaginated(request);
    }
}
