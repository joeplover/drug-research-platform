package com.aiforaso.platform.service;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.aiforaso.platform.dto.IndicatorView;
import com.aiforaso.platform.dto.RagHitView;

@Component
@ConditionalOnProperty(name = "platform.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmClient implements AiReportClient {

    private final ReportDraftBuilder reportDraftBuilder;

    public MockLlmClient(ReportDraftBuilder reportDraftBuilder) {
        this.reportDraftBuilder = reportDraftBuilder;
    }

    @Override
    public String generateReport(
            String question,
            String analysisFocus,
            List<RagHitView> evidence,
            List<IndicatorView> indicators,
            String sourceContext) {
        return reportDraftBuilder.buildFallbackReport(question, analysisFocus, evidence, indicators, sourceContext);
    }
}
