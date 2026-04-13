package com.aiforaso.platform.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "platform.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmService {

    private static final Logger log = LoggerFactory.getLogger(MockLlmService.class);

    public String chat(String systemPrompt, String userPrompt) {
        log.debug("[LLM-Mock] Returning null (mock mode)");
        return null;
    }

    public String chat(String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        return null;
    }

    public String chatWithJson(String systemPrompt, String userPrompt) {
        return null;
    }
}
