package com.devops.logcollector.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Represents an e-commerce user event payload sent to POST /api/logs.
 *
 * <pre>
 * Example JSON:
 * {
 *   "userId":    "user-42",
 *   "action":    "ADD_TO_CART",
 *   "timestamp": "2024-06-01T10:15:30Z",
 *   "productId": "prod-789"
 * }
 * </pre>
 */
public class UserEvent {

    /** Unique identifier of the user performing the action. */
    @NotBlank(message = "userId must not be blank")
    private String userId;

    /**
     * Business action performed by the user.
     * Examples: VIEW_PRODUCT, ADD_TO_CART, PURCHASE, REMOVE_FROM_CART
     */
    @NotBlank(message = "action must not be blank")
    private String action;

    /**
     * UTC timestamp of the event in ISO-8601 format.
     * Accepts both "2024-06-01T10:15:30Z" and epoch millis.
     */
    @NotNull(message = "timestamp must not be null")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'",
                timezone = "UTC")
    private Instant timestamp;

    /**
     * Identifier of the product involved in the event.
     * Optional – some actions (e.g., LOGIN) may not have an associated product.
     */
    private String productId;

    // ─── Constructors ────────────────────────────────────────────────────

    public UserEvent() {}

    public UserEvent(String userId, String action, Instant timestamp, String productId) {
        this.userId    = userId;
        this.action    = action;
        this.timestamp = timestamp;
        this.productId = productId;
    }

    // ─── Getters & Setters ───────────────────────────────────────────────

    public String getUserId()           { return userId; }
    public void   setUserId(String u)   { this.userId = u; }

    public String getAction()           { return action; }
    public void   setAction(String a)   { this.action = a; }

    public Instant getTimestamp()            { return timestamp; }
    public void    setTimestamp(Instant ts)  { this.timestamp = ts; }

    public String getProductId()            { return productId; }
    public void   setProductId(String pid)  { this.productId = pid; }

    // ─── toString (used for console logging) ─────────────────────────────

    @Override
    public String toString() {
        return "UserEvent{" +
               "userId='"    + userId    + '\'' +
               ", action='"  + action    + '\'' +
               ", timestamp=" + timestamp +
               ", productId='" + productId + '\'' +
               '}';
    }
}
