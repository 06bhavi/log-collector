package com.devops.logcollector.service;

import com.devops.logcollector.model.StatsResponse;
import com.devops.logcollector.model.StatsResponse.MinuteBucket;
import com.devops.logcollector.model.StatsResponse.ProductCount;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * StatsService — reads the HDFS newline-delimited JSON log file in a single
 * streaming pass and aggregates it into an analytics {@link StatsResponse}.
 *
 * <h3>Aggregations produced</h3>
 * <ul>
 *   <li>Total event count, purchase count, add-to-cart count
 *   <li>Distinct active user count
 *   <li>Event counts per action type
 *   <li>Top 10 most-viewed products
 *   <li>Top 10 most-purchased products
 *   <li>Purchases-per-minute for the last 30 minutes (UTC, 1-minute buckets)
 *   <li>All-events-per-minute for the last 30 minutes
 * </ul>
 *
 * <p>The method is intentionally stateless — every GET /api/stats call triggers
 * a fresh HDFS read.  For high-throughput production use, introduce a
 * scheduled cache refresh instead.
 */
@Service
public class StatsService {

    private static final Logger        log    = LoggerFactory.getLogger(StatsService.class);
    private static final ObjectMapper  MAPPER = new ObjectMapper();

    private static final DateTimeFormatter MINUTE_FMT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneOffset.UTC);

    private static final int TIMELINE_MINUTES = 30;
    private static final int TOP_N            = 10;

    @Value("${hdfs.uri:hdfs://namenode:9000}")
    private String hdfsUri;

    @Value("${hdfs.log.path:/logs/ecommerce_data.json}")
    private String hdfsLogPath;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Stream-read the HDFS log file and return aggregated analytics.
     *
     * @return a fully populated {@link StatsResponse}
     * @throws IOException if the HDFS file cannot be read
     */
    public StatsResponse buildStats() throws IOException {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsUri);
        conf.set("hadoop.security.authentication", "simple");
        conf.set("dfs.client.use.datanode.hostname", "true");

        // Accumulators
        long totalEvents    = 0;
        long totalPurchases = 0;
        long totalCart      = 0;
        Set<String>         activeUsers    = new HashSet<>();
        Map<String, Long>   eventTypeCounts = new LinkedHashMap<>();
        Map<String, Long>   viewedCounts    = new HashMap<>();
        Map<String, Long>   purchaseCounts  = new HashMap<>();
        Map<String, Long>   purchaseByMin   = new TreeMap<>();
        Map<String, Long>   eventsByMin     = new TreeMap<>();

        // Build the 30-minute bucket spine so gaps show as 0
        Instant now = Instant.now();
        for (int i = TIMELINE_MINUTES - 1; i >= 0; i--) {
            String bucket = MINUTE_FMT.format(now.minusSeconds(i * 60L));
            purchaseByMin.put(bucket, 0L);
            eventsByMin.put(bucket,   0L);
        }

        try (FileSystem fs = FileSystem.newInstance(conf)) {
            Path logPath = new Path(hdfsLogPath);
            if (!fs.exists(logPath)) {
                log.warn("HDFS log file not found: {} — returning empty stats.", hdfsLogPath);
                return emptyResponse();
            }

            try (FSDataInputStream raw = fs.open(logPath);
                 BufferedReader reader = new BufferedReader(
                         new InputStreamReader(raw, StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.strip();
                    if (line.isEmpty()) continue;

                    JsonNode node;
                    try {
                        node = MAPPER.readTree(line);
                    } catch (Exception e) {
                        log.debug("Skipping unparseable line: {}", line);
                        continue;
                    }

                    String action    = text(node, "action");
                    String userId    = text(node, "userId");
                    String productId = text(node, "productId");
                    String tsRaw     = text(node, "timestamp");

                    if (action == null || userId == null) continue;

                    // ── KPIs ──────────────────────────────────────────────
                    totalEvents++;
                    if ("purchase".equals(action))    totalPurchases++;
                    if ("add_to_cart".equals(action)) totalCart++;
                    activeUsers.add(userId);

                    // ── Event type histogram ───────────────────────────────
                    eventTypeCounts.merge(action, 1L, Long::sum);

                    // ── Product views ──────────────────────────────────────
                    if ("item_viewed".equals(action) && productId != null) {
                        viewedCounts.merge(productId, 1L, Long::sum);
                    }
                    if ("purchase".equals(action) && productId != null) {
                        purchaseCounts.merge(productId, 1L, Long::sum);
                    }

                    // ── Time-series buckets (last 30 min only) ─────────────
                    if (tsRaw != null) {
                        try {
                            Instant ts = Instant.parse(tsRaw);
                            if (!ts.isBefore(now.minusSeconds(TIMELINE_MINUTES * 60L))) {
                                String bucket = MINUTE_FMT.format(ts);
                                eventsByMin.merge(bucket, 1L, Long::sum);
                                if ("purchase".equals(action)) {
                                    purchaseByMin.merge(bucket, 1L, Long::sum);
                                }
                            }
                        } catch (Exception ignored) { /* malformed ts — skip */ }
                    }
                }
            }
        }

        // ── Build response ────────────────────────────────────────────────────
        StatsResponse resp = new StatsResponse();
        resp.setTotalEvents(totalEvents);
        resp.setTotalPurchases(totalPurchases);
        resp.setTotalAddToCart(totalCart);
        resp.setActiveUsers(activeUsers.size());

        // Sort event type map by count desc for readability
        Map<String, Long> sortedTypes = eventTypeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue,
                        (a, b) -> a, LinkedHashMap::new));
        resp.setEventTypeCounts(sortedTypes);

        resp.setTopViewedItems(topN(viewedCounts, TOP_N));
        resp.setTopPurchasedItems(topN(purchaseCounts, TOP_N));

        resp.setPurchasesPerMinute(toBucketList(purchaseByMin));
        resp.setEventsPerMinute(toBucketList(eventsByMin));
        resp.setGeneratedAt(now.toString());

        log.info("Stats computed: {} events, {} purchases, {} users",
                totalEvents, totalPurchases, activeUsers.size());
        return resp;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static String text(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n == null || n.isNull()) ? null : n.asText();
    }

    private static List<ProductCount> topN(Map<String, Long> counts, int n) {
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(n)
                .map(e -> new ProductCount(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private static List<MinuteBucket> toBucketList(Map<String, Long> buckets) {
        return buckets.entrySet().stream()
                .map(e -> new MinuteBucket(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private StatsResponse emptyResponse() {
        StatsResponse r = new StatsResponse();
        r.setTotalEvents(0);
        r.setTotalPurchases(0);
        r.setTotalAddToCart(0);
        r.setActiveUsers(0);
        r.setEventTypeCounts(Collections.emptyMap());
        r.setTopViewedItems(Collections.emptyList());
        r.setTopPurchasedItems(Collections.emptyList());
        r.setPurchasesPerMinute(Collections.emptyList());
        r.setEventsPerMinute(Collections.emptyList());
        r.setGeneratedAt(Instant.now().toString());
        return r;
    }
}
