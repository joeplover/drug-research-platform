package com.aiforaso.platform.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiforaso.platform.dto.GraphQueryRequest;
import com.aiforaso.platform.dto.KnowledgeGraphResponse;
import com.aiforaso.platform.service.KnowledgeGraphService;

@RestController
@RequestMapping("/api/knowledge-graph")
public class KnowledgeGraphController {

    private final KnowledgeGraphService knowledgeGraphService;

    public KnowledgeGraphController(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @GetMapping
    public KnowledgeGraphResponse buildGraph() {
        return knowledgeGraphService.buildGraph();
    }

    @GetMapping("/literatures/{literatureId}")
    public KnowledgeGraphResponse buildGraphForLiterature(@PathVariable Long literatureId) {
        return knowledgeGraphService.buildGraphForLiterature(literatureId);
    }

    @PostMapping("/query")
    public KnowledgeGraphResponse query(@RequestBody(required = false) GraphQueryRequest request) {
        GraphQueryRequest safeRequest = request == null ? new GraphQueryRequest(null, null, null, null) : request;
        return knowledgeGraphService.queryGraph(
                safeRequest.literatureId(),
                safeRequest.keyword(),
                safeRequest.nodeType(),
                safeRequest.reviewStatus());
    }
}
