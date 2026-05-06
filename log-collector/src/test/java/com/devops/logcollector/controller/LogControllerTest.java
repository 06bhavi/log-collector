package com.devops.logcollector.controller;

import com.devops.logcollector.model.UserEvent;
import com.devops.logcollector.service.HdfsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Slice tests for {@link LogController}.
 *
 * <p>{@code @WebMvcTest} wires only the web layer (no HDFS, no real Spring context).
 * {@code @MockBean HdfsService} satisfies the dependency without touching Hadoop.</p>
 *
 * Verifies:
 *  - Happy path returns 202 with correct body
 *  - Missing required field returns 400
 *  - Completely empty body returns 400
 */
@WebMvcTest(controllers = {LogController.class, GlobalExceptionHandler.class})
class LogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Mock the HDFS service so tests never need a real Hadoop cluster.
     * appendLine() is stubbed to do nothing (void method default is no-op in Mockito,
     * but being explicit avoids confusion).
     */
    @MockBean
    private HdfsService hdfsService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    // ── Happy path ────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/logs — valid payload → 202 Accepted")
    void givenValidEvent_whenPost_thenAccepted() throws Exception {

        // Stub HDFS write — succeed silently
        doNothing().when(hdfsService).appendLine(anyString());

        UserEvent event = new UserEvent(
                "user-42",
                "ADD_TO_CART",
                Instant.parse("2024-06-01T10:15:30Z"),
                "prod-789"
        );

        mockMvc.perform(post("/api/logs")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(event)))
               .andExpect(status().isAccepted())
               .andExpect(jsonPath("$.status").value("accepted"))
               .andExpect(jsonPath("$.userId").value("user-42"))
               .andExpect(jsonPath("$.action").value("ADD_TO_CART"));
    }

    @Test
    @DisplayName("POST /api/logs — event without productId (optional field) → 202 Accepted")
    void givenEventWithoutProductId_whenPost_thenAccepted() throws Exception {

        doNothing().when(hdfsService).appendLine(anyString());

        UserEvent event = new UserEvent("user-99", "LOGIN", Instant.now(), null);

        mockMvc.perform(post("/api/logs")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(event)))
               .andExpect(status().isAccepted());
    }

    // ── Validation failures ───────────────────────────────────────────────

    @Test
    @DisplayName("POST /api/logs — missing userId → 400 Bad Request")
    void givenMissingUserId_whenPost_thenBadRequest() throws Exception {

        String json = """
                {
                  "action":    "VIEW_PRODUCT",
                  "timestamp": "2024-06-01T10:15:30Z",
                  "productId": "prod-1"
                }
                """;

        mockMvc.perform(post("/api/logs")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.message").value("Validation failed"))
               .andExpect(jsonPath("$.details[0]").value("userId must not be blank"));
    }

    @Test
    @DisplayName("POST /api/logs — missing timestamp → 400 Bad Request")
    void givenMissingTimestamp_whenPost_thenBadRequest() throws Exception {

        String json = """
                {
                  "userId": "user-10",
                  "action": "PURCHASE"
                }
                """;

        mockMvc.perform(post("/api/logs")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(json))
               .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/logs — empty body → 400 Bad Request")
    void givenEmptyBody_whenPost_thenBadRequest() throws Exception {

        mockMvc.perform(post("/api/logs")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content("{}"))
               .andExpect(status().isBadRequest());
    }
}
