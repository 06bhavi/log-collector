package com.devops.logcollector.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("service", "log-collector API");
        body.put("message", "The API is running. Please use /api/logs for ingestion and /api/stats for analytics.");
        body.put("dashboard_url", "http://localhost:3000");
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(body);
    }
}
