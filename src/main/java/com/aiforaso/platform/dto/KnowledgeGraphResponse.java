package com.aiforaso.platform.dto;

import java.util.List;

public record KnowledgeGraphResponse(
        List<GraphNode> nodes,
        List<GraphEdge> edges) {

    public record GraphNode(String id, String label, String type) {
    }

    public record GraphEdge(String source, String target, String relation) {
    }
}
