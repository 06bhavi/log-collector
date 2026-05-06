package com.devops.logcollector.controller;

import com.devops.logcollector.model.StatsResponse;
import com.devops.logcollector.service.StatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller that exposes the analytics aggregation endpoint.
 *
 * <pre>
 * GET /api/stats
 * Response: application/json — StatsResponse
 * </pre>
 *
 * <p>CORS is opened for all origins so the React analytics-dashboard can call
 * this endpoint both in development (Vite dev server, port 5173) and in
 * production (Nginx container, port 80).
 */
@RestController
@RequestMapping("/api/stats")
@CrossOrigin(origins = "*")          // tighten to specific origin in production
public class StatsController {

    private static final Logger log = LoggerFactory.getLogger(StatsController.class);

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /**
     * Read the HDFS log file and return aggregated e-commerce analytics.
     *
     * @return 200 OK with a {@link StatsResponse} body, or 503 if HDFS is unreachable.
     */
    @GetMapping
    public ResponseEntity<?> getStats() {
        try {
            StatsResponse stats = statsService.buildStats();
            return ResponseEntity.ok(stats);
        } catch (IOException e) {
            log.error("Failed to read HDFS log for stats: {}", e.getMessage(), e);
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("status",    "error");
            err.put("detail",    "Could not read analytics data from HDFS: " + e.getMessage());
            err.put("timestamp", Instant.now().toString());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(err);
        }
    }
}
