package com.aiforaso.platform.service;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.aiforaso.platform.config.AiProviderProperties;
import com.aiforaso.platform.dto.HealthComponentView;

@Service
public class ChunkEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkEmbeddingService.class);
    private static final int FALLBACK_DIMENSIONS = 64;
    private static final AtomicBoolean OLLAMA_FALLBACK_LOGGED = new AtomicBoolean(false);

    private final RestClient restClient;
    private final AiProviderProperties aiProviderProperties;
    private final AtomicReference<String> lastMode = new AtomicReference<>("UNKNOWN");

    public ChunkEmbeddingService(RestClient.Builder restClientBuilder, AiProviderProperties aiProviderProperties) {
        this.restClient = restClientBuilder
                .baseUrl(aiProviderProperties.getEmbedding().getBaseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.aiProviderProperties = aiProviderProperties;
    }

    public double[] embed(String text) {
        if (text == null || text.isBlank()) {
            return new double[FALLBACK_DIMENSIONS];
        }

        String provider = aiProviderProperties.getEmbedding().getProvider();
        if (provider != null && "ollama".equalsIgnoreCase(provider)) {
            try {
                double[] vector = requestOllamaEmbedding(text);
                lastMode.set("OLLAMA");
                return vector;
            } catch (Exception exception) {
                if (!aiProviderProperties.getEmbedding().isFallbackEnabled()) {
                    throw new IllegalStateException(
                            "Ollama embedding call failed. Please check whether Ollama is running and model "
                                    + aiProviderProperties.getEmbedding().getModel()
                                    + " is available at "
                                    + aiProviderProperties.getEmbedding().getBaseUrl(),
                            exception);
                }
                if (OLLAMA_FALLBACK_LOGGED.compareAndSet(false, true)) {
                    log.warn(
                            "Ollama embedding is unavailable at {} using model {}. The system will temporarily fall back to local deterministic embeddings. Cause: {}",
                            aiProviderProperties.getEmbedding().getBaseUrl(),
                            aiProviderProperties.getEmbedding().getModel(),
                            exception.getMessage());
                }
            }
        }

        lastMode.set("LOCAL_FALLBACK");
        return fallbackEmbed(text);
    }

    public String getLastMode() {
        return lastMode.get();
    }

    public HealthComponentView healthComponent() {
        long start = System.currentTimeMillis();
        try {
            if (!"ollama".equalsIgnoreCase(aiProviderProperties.getEmbedding().getProvider())) {
                return new HealthComponentView(
                        "Embedding",
                        "UP",
                        "当前使用 " + aiProviderProperties.getEmbedding().getProvider() + " 提供 embedding",
                        System.currentTimeMillis() - start);
            }
            requestOllamaEmbedding("health check");
            return new HealthComponentView(
                    "Embedding",
                    "UP",
                    "Ollama embedding 可用，模型=" + aiProviderProperties.getEmbedding().getModel(),
                    System.currentTimeMillis() - start);
        } catch (Exception exception) {
            String detail = aiProviderProperties.getEmbedding().isFallbackEnabled()
                    ? "Ollama 不可用，当前将降级为本地 embedding。原因: " + exception.getMessage()
                    : "Ollama 不可用，且未开启降级。原因: " + exception.getMessage();
            return new HealthComponentView(
                    "Embedding",
                    aiProviderProperties.getEmbedding().isFallbackEnabled() ? "DEGRADED" : "DOWN",
                    detail,
                    System.currentTimeMillis() - start);
        }
    }

    public double[] cosineSourceVector(String text, String storedEmbeddingJson) {
        double[] storedVector = deserialize(storedEmbeddingJson);
        if (storedVector.length > 0) {
            return storedVector;
        }
        return embed(text);
    }

    public double cosineSimilarity(double[] left, double[] right) {
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int index = 0; index < Math.min(left.length, right.length); index++) {
            dot += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    public String serialize(double[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        return Arrays.stream(vector)
                .map(value -> Math.round(value * 1_000_000d) / 1_000_000d)
                .mapToObj(Double::toString)
                .collect(Collectors.joining(",", "[", "]"));
    }

    public double[] deserialize(String raw) {
        if (raw == null || raw.isBlank()) {
            return new double[0];
        }
        String normalized = raw.trim();
        if (!normalized.startsWith("[") || !normalized.endsWith("]")) {
            return new double[0];
        }
        String body = normalized.substring(1, normalized.length() - 1).trim();
        if (body.isBlank()) {
            return new double[0];
        }
        String[] parts = body.split(",");
        double[] vector = new double[parts.length];
        for (int index = 0; index < parts.length; index++) {
            vector[index] = Double.parseDouble(parts[index].trim());
        }
        return vector;
    }

    private double[] requestOllamaEmbedding(String text) {
        OllamaEmbeddingResponse response = restClient.post()
                .uri("/api/embed")
                .body(new OllamaEmbeddingRequest(aiProviderProperties.getEmbedding().getModel(), text))
                .retrieve()
                .body(OllamaEmbeddingResponse.class);

        if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
            throw new IllegalStateException("Ollama embedding response is empty");
        }

        List<Double> values = response.embeddings().get(0);
        double[] vector = new double[values.size()];
        for (int index = 0; index < values.size(); index++) {
            vector[index] = values.get(index);
        }
        return vector;
    }

    private double[] fallbackEmbed(String text) {
        double[] vector = new double[FALLBACK_DIMENSIONS];
        Arrays.stream(text.toLowerCase(Locale.ROOT).split("\\W+"))
                .filter(token -> token.length() > 1)
                .forEach(token -> {
                    int index = Math.floorMod(token.hashCode(), FALLBACK_DIMENSIONS);
                    vector[index] += 1.0;
                });

        normalize(vector);
        return vector;
    }

    private void normalize(double[] vector) {
        double sumSquares = 0.0;
        for (double value : vector) {
            sumSquares += value * value;
        }
        if (sumSquares == 0.0) {
            return;
        }
        double norm = Math.sqrt(sumSquares);
        for (int index = 0; index < vector.length; index++) {
            vector[index] = vector[index] / norm;
        }
    }

    private record OllamaEmbeddingRequest(String model, String input) {
    }

    private record OllamaEmbeddingResponse(List<List<Double>> embeddings) {
    }
}
