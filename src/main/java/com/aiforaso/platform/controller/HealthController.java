package com.aiforaso.platform.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.aiforaso.platform.dto.HealthView;
import com.aiforaso.platform.service.PlatformHealthService;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final PlatformHealthService platformHealthService;

    public HealthController(PlatformHealthService platformHealthService) {
        this.platformHealthService = platformHealthService;
    }

    @GetMapping
    public HealthView health() {
        return platformHealthService.health();
    }
}
