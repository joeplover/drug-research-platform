package com.aiforaso.platform.service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.aiforaso.platform.dto.HealthComponentView;
import com.aiforaso.platform.dto.HealthView;

@Service
public class PlatformHealthService {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final MilvusVectorStoreService milvusVectorStoreService;
    private final ChunkEmbeddingService chunkEmbeddingService;

    public PlatformHealthService(
            DataSource dataSource,
            StringRedisTemplate stringRedisTemplate,
            MilvusVectorStoreService milvusVectorStoreService,
            ChunkEmbeddingService chunkEmbeddingService) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.stringRedisTemplate = stringRedisTemplate;
        this.milvusVectorStoreService = milvusVectorStoreService;
        this.chunkEmbeddingService = chunkEmbeddingService;
    }

    public HealthView health() {
        List<HealthComponentView> components = new ArrayList<>();
        components.add(checkMysql());
        components.add(checkRedis());
        components.add(milvusVectorStoreService.healthComponent());
        components.add(chunkEmbeddingService.healthComponent());

        boolean allUp = components.stream().allMatch(component -> "UP".equals(component.status()));
        return new HealthView(
                allUp ? "UP" : "DEGRADED",
                "aso-platform",
                OffsetDateTime.now(),
                components);
    }

    private HealthComponentView checkMysql() {
        long start = System.currentTimeMillis();
        try {
            Integer result = jdbcTemplate.queryForObject("select 1", Integer.class);
            return new HealthComponentView(
                    "MySQL",
                    Integer.valueOf(1).equals(result) ? "UP" : "DOWN",
                    Integer.valueOf(1).equals(result) ? "Database connection is healthy" : "Database returned an unexpected result",
                    System.currentTimeMillis() - start);
        } catch (Exception exception) {
            return new HealthComponentView(
                    "MySQL",
                    "DOWN",
                    simplifyMessage(exception),
                    System.currentTimeMillis() - start);
        }
    }

    private HealthComponentView checkRedis() {
        long start = System.currentTimeMillis();
        try {
            String pong = stringRedisTemplate.getConnectionFactory() == null
                    ? null
                    : stringRedisTemplate.getConnectionFactory().getConnection().ping();
            return new HealthComponentView(
                    "Redis",
                    "PONG".equalsIgnoreCase(pong) ? "UP" : "DOWN",
                    "PONG".equalsIgnoreCase(pong) ? "Redis connection is healthy" : "Redis returned an unexpected result: " + pong,
                    System.currentTimeMillis() - start);
        } catch (Exception exception) {
            return new HealthComponentView(
                    "Redis",
                    "DOWN",
                    simplifyMessage(exception),
                    System.currentTimeMillis() - start);
        }
    }

    private String simplifyMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
