package com.aiforaso.platform.dto;

public record WorkflowProcessRequest(
        String question,
        String analysisFocus,
        boolean reingest,
        boolean reextract,
        boolean onlyConfirmedIndicators) {
}
