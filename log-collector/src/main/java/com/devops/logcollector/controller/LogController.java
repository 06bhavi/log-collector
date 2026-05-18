package com.devops.logcollector.controller;

import com.devops.logcollector.model.UserEvent;
import com.devops.logcollector.service.HdfsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
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
 * REST controller that exposes the event-log ingestion endpoint.
 * On each request the event is:
 *   Validated via Bean Validation (@Valid)
 *   Serialised back to a compact JSON string
 *   Appended as a newline-delimited record to HDFS via {@link HdfsService}
 *   Also logged to stdout for immediate visibility in {@code docker logs}
 * 
 */
@RestController
@RequestMapping("/api/logs")
public class LogController {

    private static final Logger log = LoggerFactory.getLogger(LogController.class);

    private final HdfsService  hdfsService;
    private final ObjectMapper objectMapper;

    /**
     * Spring injects both beans automatically — no @Autowired on fields needed.
     *
     * @param hdfsService  the singleton HDFS append service
     * @param objectMapper Jackson mapper (auto-configured by Spring Boot)
     */
    public LogController(HdfsService hdfsService, ObjectMapper objectMapper) {
        this.hdfsService  = hdfsService;
        this.objectMapper = objectMapper;
    }

    /**
     * Accepts a {@link UserEvent} payload, writes it to HDFS, and returns
     * a 202 Accepted acknowledgement.
     *
     * @param event the deserialized and validated user-event payload
     * @return 202 Accepted on success, 400 Bad Request on validation failure
     *         (handled by {@link GlobalExceptionHandler}), or 502 if the HDFS
     *         write fails.
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> ingestLog(@Valid @RequestBody UserEvent event) {

        // ── 1. Console / structured log output ────────────────────────────
        log.info("=================================================");
        log.info("  Received e-commerce event");
        log.info("  userId    : {}", event.getUserId());
        log.info("  action    : {}", event.getAction());
        log.info("  timestamp : {}", event.getTimestamp());
        log.info("  productId : {}", event.getProductId() != null
                                        ? event.getProductId() : "(none)");
        log.info("=================================================");

        // ── 2. Serialise event to a compact JSON line ──────────────────────
        String jsonLine;
        try {
            jsonLine = objectMapper.writeValueAsString(event);
        } catch (IOException e) {
            log.error("Failed to serialise event to JSON: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody("Serialisation error: " + e.getMessage()));
        }

        // ── 3. Append to HDFS ─────────────────────────────────────────────
        try {
            hdfsService.appendLine(jsonLine);
            log.info("  → Written to HDFS successfully.");
        } catch (IOException e) {
            // Log the failure but do NOT silently swallow it — return 502 so
            // mock-storefront's retry logic can kick in.
            log.error("HDFS write failed for event userId={} action={}: {}",
                    event.getUserId(), event.getAction(), e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(errorBody("HDFS write failed: " + e.getMessage()));
        }

        // ── 4. Build acknowledgement response ─────────────────────────────
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",     "accepted");
        body.put("message",    "Event received and written to HDFS successfully");
        body.put("userId",     event.getUserId());
        body.put("action",     event.getAction());
        body.put("receivedAt", Instant.now().toString());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(body);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Map<String, Object> errorBody(String detail) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("status", "error");
        err.put("detail", detail);
        err.put("timestamp", Instant.now().toString());
        return err;
    }
}
