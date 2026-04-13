package com.aiforaso.platform.service;

import java.util.List;

import com.aiforaso.platform.dto.IndicatorView;
import com.aiforaso.platform.dto.RagHitView;

public interface AiReportClient {

    String generateReport(
            String question,
            String analysisFocus,
            List<RagHitView> evidence,
            List<IndicatorView> indicators,
            String sourceContext);
}
