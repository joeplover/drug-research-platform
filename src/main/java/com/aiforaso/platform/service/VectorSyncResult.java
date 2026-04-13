package com.aiforaso.platform.service;

public record VectorSyncResult(
        String status,
        int syncedCount,
        String detail) {
}
