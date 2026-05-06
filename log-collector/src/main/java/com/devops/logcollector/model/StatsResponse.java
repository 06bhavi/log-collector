package com.devops.logcollector.model;

import java.util.List;
import java.util.Map;

/**
 * Aggregated analytics payload returned by GET /api/stats.
 *
 * <p>All data is derived by reading and parsing the HDFS newline-delimited JSON
 * log file in one pass, so no separate store is needed.
 */
public class StatsResponse {

    // ── High-level KPIs ──────────────────────────────────────────────────────

    /** Total number of events recorded in the HDFS log file. */
    private long totalEvents;

    /** Total number of "purchase" events. */
    private long totalPurchases;

    /** Total number of "add_to_cart" events. */
    private long totalAddToCart;

    /** Count of distinct userIds seen in the log. */
    private long activeUsers;

    // ── Breakdown by event type ──────────────────────────────────────────────

    /**
     * Count of events per action name.
     * Example: { "item_viewed": 1820, "add_to_cart": 403, "purchase": 162, … }
     */
    private Map<String, Long> eventTypeCounts;

    // ── Top products ─────────────────────────────────────────────────────────

    /**
     * Top 10 most-viewed products (action = "item_viewed"), sorted descending.
     */
    private List<ProductCount> topViewedItems;

    /**
     * Top 10 most-purchased products (action = "purchase"), sorted descending.
     */
    private List<ProductCount> topPurchasedItems;

    // ── Time-series ──────────────────────────────────────────────────────────

    /**
     * Purchase counts grouped into 1-minute buckets for the last 30 minutes.
     * Sorted ascending by {@code minute}.
     */
    private List<MinuteBucket> purchasesPerMinute;

    /**
     * All event counts grouped into 1-minute buckets for the last 30 minutes.
     */
    private List<MinuteBucket> eventsPerMinute;

    /** ISO-8601 UTC timestamp of when this response was generated. */
    private String generatedAt;

    // ── Constructors ─────────────────────────────────────────────────────────

    public StatsResponse() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public long getTotalEvents()                             { return totalEvents; }
    public void setTotalEvents(long v)                       { this.totalEvents = v; }

    public long getTotalPurchases()                          { return totalPurchases; }
    public void setTotalPurchases(long v)                    { this.totalPurchases = v; }

    public long getTotalAddToCart()                          { return totalAddToCart; }
    public void setTotalAddToCart(long v)                    { this.totalAddToCart = v; }

    public long getActiveUsers()                             { return activeUsers; }
    public void setActiveUsers(long v)                       { this.activeUsers = v; }

    public Map<String, Long> getEventTypeCounts()            { return eventTypeCounts; }
    public void setEventTypeCounts(Map<String, Long> v)      { this.eventTypeCounts = v; }

    public List<ProductCount> getTopViewedItems()            { return topViewedItems; }
    public void setTopViewedItems(List<ProductCount> v)      { this.topViewedItems = v; }

    public List<ProductCount> getTopPurchasedItems()         { return topPurchasedItems; }
    public void setTopPurchasedItems(List<ProductCount> v)   { this.topPurchasedItems = v; }

    public List<MinuteBucket> getPurchasesPerMinute()        { return purchasesPerMinute; }
    public void setPurchasesPerMinute(List<MinuteBucket> v)  { this.purchasesPerMinute = v; }

    public List<MinuteBucket> getEventsPerMinute()           { return eventsPerMinute; }
    public void setEventsPerMinute(List<MinuteBucket> v)     { this.eventsPerMinute = v; }

    public String getGeneratedAt()                           { return generatedAt; }
    public void setGeneratedAt(String v)                     { this.generatedAt = v; }

    // ── Nested value types ───────────────────────────────────────────────────

    /** A product and its associated event count. */
    public static class ProductCount {
        private String productId;
        private long   count;

        public ProductCount() {}
        public ProductCount(String productId, long count) {
            this.productId = productId;
            this.count     = count;
        }

        public String getProductId() { return productId; }
        public void   setProductId(String v) { this.productId = v; }
        public long   getCount()     { return count; }
        public void   setCount(long v) { this.count = v; }
    }

    /** A 1-minute time bucket with an event count. */
    public static class MinuteBucket {
        /** Format: {@code "HH:mm"} in UTC */
        private String minute;
        private long   count;

        public MinuteBucket() {}
        public MinuteBucket(String minute, long count) {
            this.minute = minute;
            this.count  = count;
        }

        public String getMinute() { return minute; }
        public void   setMinute(String v) { this.minute = v; }
        public long   getCount()  { return count; }
        public void   setCount(long v) { this.count = v; }
    }
}
