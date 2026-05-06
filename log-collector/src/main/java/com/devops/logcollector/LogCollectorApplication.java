package com.devops.logcollector;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry-point for the Log Collector microservice.
 * <p>
 * Accepts e-commerce user-event payloads via a REST API, validates them,
 * and (for now) prints them to the console.
 * </p>
 */
@SpringBootApplication
public class LogCollectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogCollectorApplication.class, args);
    }
}
