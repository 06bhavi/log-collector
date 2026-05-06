# ════════════════════════════════════════════════════════════════════════════
#  Dockerfile — log-collector
#  Stage 1: Maven build
#  Stage 2: Eclipse Temurin 17 JRE (slim Alpine variant)
# ════════════════════════════════════════════════════════════════════════════

# ── Stage 1: Build the application using Maven ───────────────────────────────
FROM maven:3.9-eclipse-temurin-17-alpine AS builder
WORKDIR /build

# Copy pom.xml and resolve dependencies first (better layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the JAR
COPY src ./src
RUN mvn package -DskipTests -B

# ── Stage 2: Runtime image ───────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# Metadata labels (OCI standard)
LABEL maintainer="devops-team"
LABEL org.opencontainers.image.title="log-collector"
LABEL org.opencontainers.image.description="E-commerce event log collector — Spring Boot REST API"
LABEL org.opencontainers.image.version="1.0.0"

# ── Create a non-root user for security best practices ───────────────────────
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# ── Working directory inside the container ───────────────────────────────────
WORKDIR /app

# ── Copy the uber-jar produced by the builder stage ──────────────────────────
COPY --from=builder /build/target/log-collector.jar app.jar

# ── Ensure the non-root user owns the application files ──────────────────────
RUN chown -R appuser:appgroup /app

# ── Switch to non-root user ───────────────────────────────────────────────────
USER appuser

# ── Expose the default Spring Boot port ──────────────────────────────────────
EXPOSE 8080

# ── JVM tuning: container-aware heap sizing + exit on OOM ────────────────────
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError"

# ── Healthcheck (Docker will mark container unhealthy if this fails) ──────────
HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health 2>/dev/null | \
        grep -q '"status":"UP"' || exit 1

# ── Entrypoint ───────────────────────────────────────────────────────────────
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
