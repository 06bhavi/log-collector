package com.devops.logcollector.service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * HdfsService — thread-safe Spring singleton that appends newline-delimited
 * JSON records to a single HDFS file.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>A single {@link FileSystem} handle is opened at startup and reused
 *       for the lifetime of the application (no per-request connection churn).
 *   <li>Writes are {@code synchronized} so concurrent HTTP requests never
 *       interleave partial JSON lines in the output file.
 *   <li>The target path is created (including parent directories) on first
 *       use if it doesn't already exist.
 *   <li>Both {@code hdfs.uri} and {@code hdfs.log.path} are externalised as
 *       Spring properties so they can be overridden via environment variables
 *       without a code change.
 * </ul>
 */
@Service
public class HdfsService {

    private static final Logger log = LoggerFactory.getLogger(HdfsService.class);

    /** HDFS NameNode URI, e.g. {@code hdfs://namenode:9000} */
    @Value("${hdfs.uri:hdfs://namenode:9000}")
    private String hdfsUri;

    /** Absolute HDFS path where log lines are appended. */
    @Value("${hdfs.log.path:/logs/ecommerce_data.json}")
    private String hdfsLogPath;

    private FileSystem fileSystem;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Open a connection to HDFS when the Spring context is ready.
     * Creates the parent directory + an empty file if the path does not exist.
     */
    @PostConstruct
    public void init() throws IOException {
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", hdfsUri);

        // Disable Kerberos / SASL for a dev cluster
        conf.set("hadoop.security.authentication", "simple");
        conf.set("dfs.client.use.datanode.hostname", "true");

        fileSystem = FileSystem.get(conf);
        log.info("HDFS FileSystem connected → {}", hdfsUri);

        ensurePathExists();
    }

    /**
     * Close the HDFS {@link FileSystem} handle on application shutdown so all
     * buffers are flushed and resources are released cleanly.
     */
    @PreDestroy
    public void destroy() {
        if (fileSystem != null) {
            try {
                fileSystem.close();
                log.info("HDFS FileSystem closed.");
            } catch (IOException e) {
                log.warn("Error closing HDFS FileSystem: {}", e.getMessage());
            }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Appends a single JSON string as a newline-terminated record to the
     * configured HDFS file.  Thread-safe — callers from concurrent request
     * threads will be serialised at this method boundary.
     *
     * @param jsonLine a single JSON object string (no embedded newlines)
     * @throws IOException if the HDFS write fails
     */
    public synchronized void appendLine(String jsonLine) throws IOException {
        Path path = new Path(hdfsLogPath);
        byte[] bytes = (jsonLine + "\n").getBytes(StandardCharsets.UTF_8);

        // FSDataOutputStream.append() requires dfs.support.append=true,
        // which bde2020/hadoop-namenode enables by default.
        try (FSDataOutputStream out = fileSystem.append(path)) {
            out.write(bytes);
            out.flush();
        }

        log.debug("Appended {} bytes to HDFS:{}", bytes.length, hdfsLogPath);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Create the target file (and any missing parent directories) if it does
     * not already exist.  Called once at startup.
     */
    private void ensurePathExists() throws IOException {
        Path logPath = new Path(hdfsLogPath);
        Path parentDir = logPath.getParent();

        if (!fileSystem.exists(parentDir)) {
            fileSystem.mkdirs(parentDir);
            log.info("Created HDFS directory: {}", parentDir);
        }

        if (!fileSystem.exists(logPath)) {
            // Create an empty file so append() has something to append to
            fileSystem.create(logPath, false).close();
            log.info("Created HDFS log file: {}", logPath);
        } else {
            log.info("HDFS log file already exists: {}", logPath);
        }
    }
}
