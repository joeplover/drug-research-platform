package com.aiforaso.platform.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class LiteratureChunkingService {

    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    private static final int TARGET_CHUNK_LENGTH = 800;
    private static final int MIN_CHUNK_LENGTH = 180;

    public List<String> chunk(String content) {
        String normalized = WHITESPACE.matcher(content == null ? "" : content).replaceAll(" ").trim();
        if (normalized.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + TARGET_CHUNK_LENGTH);
            if (end < normalized.length()) {
                int lastSentence = Math.max(normalized.lastIndexOf(". ", end), normalized.lastIndexOf("; ", end));
                if (lastSentence > start + MIN_CHUNK_LENGTH) {
                    end = lastSentence + 1;
                }
            }

            String chunk = normalized.substring(start, end).trim();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            start = end;
        }
        return chunks;
    }
}
