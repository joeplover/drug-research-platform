package com.aiforaso.platform.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import com.aiforaso.platform.config.AiProviderProperties;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

@Service
@ConditionalOnExpression("'${platform.ai.provider:mock}'.toLowerCase() != 'mock'")
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(120);

    private final AiProviderProperties aiProviderProperties;
    private final ChatLanguageModel chatModel;

    public LlmService(AiProviderProperties aiProviderProperties) {
        this.aiProviderProperties = aiProviderProperties;
        
        String apiKey = aiProviderProperties.getApiKey();
        String baseUrl = aiProviderProperties.getBaseUrl();
        String model = aiProviderProperties.getModel();

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[LLM] API key is not configured. Set DEEPSEEK_API_KEY environment variable.");
        }

        log.info("[LLM] Initializing with provider: {}, model: {}, baseUrl: {}", 
                aiProviderProperties.getProvider(), model, baseUrl);

        this.chatModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(0.3)
                .maxTokens(2000)
                .timeout(DEFAULT_TIMEOUT)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, 0.3, 2000);
    }

    public String chat(String systemPrompt, String userPrompt, double temperature, int maxTokens) {
        try {
            log.debug("[LLM] Calling chat with temperature={}, maxTokens={}", temperature, maxTokens);
            
            ChatLanguageModel model = OpenAiChatModel.builder()
                    .apiKey(aiProviderProperties.getApiKey())
                    .baseUrl(aiProviderProperties.getBaseUrl())
                    .modelName(aiProviderProperties.getModel())
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .timeout(DEFAULT_TIMEOUT)
                    .build();

            String fullPrompt = systemPrompt + "\n\n" + userPrompt;
            String response = model.generate(fullPrompt);

            if (response == null || response.isBlank()) {
                log.warn("[LLM] Blank response from model");
                return null;
            }

            log.debug("[LLM] Response received, length={}", response.length());
            return response.trim();
        } catch (Exception e) {
            log.error("[LLM] Call failed: {}", e.getMessage(), e);
            return null;
        }
    }

    public String chatWithJson(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, 0.1, 3000);
    }
}
