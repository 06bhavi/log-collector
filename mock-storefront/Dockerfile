# ── Base ──────────────────────────────────────────────────────────────────────
FROM python:3.10-slim AS base

# Prevents Python from writing .pyc files and enables unbuffered stdout/stderr
# (so logs appear immediately in `docker logs`)
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1 \
    # pip: no cache dir inside the image, treat warnings as errors
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# ── System dependencies ───────────────────────────────────────────────────────
# curl is needed only for the HEALTHCHECK; everything else is already in slim.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# ── Non-root user ─────────────────────────────────────────────────────────────
RUN groupadd --system appgroup \
    && useradd  --system --gid appgroup --no-create-home appuser

# ── Working directory ─────────────────────────────────────────────────────────
WORKDIR /app

# ── Install Python dependencies ───────────────────────────────────────────────
# Copy requirements first to leverage Docker layer caching:
# dependencies are re-installed only when requirements.txt changes.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# ── Copy application source ───────────────────────────────────────────────────
# .dockerignore excludes .venv/, __pycache__/, .env, tests/, etc.
COPY app/ ./app/

# ── Ownership ─────────────────────────────────────────────────────────────────
RUN chown -R appuser:appgroup /app

# ── Drop to non-root ──────────────────────────────────────────────────────────
USER appuser

# ── Port ─────────────────────────────────────────────────────────────────────
EXPOSE 8000

# ── Healthcheck ───────────────────────────────────────────────────────────────
# Checks the /healthz liveness probe every 30 s.
# start-period gives the app time to boot before the first check.
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -fsSL http://localhost:8000/healthz || exit 1

# ── Entrypoint ────────────────────────────────────────────────────────────────
# Use exec form (no shell wrapper) so SIGTERM reaches uvicorn directly,
# enabling graceful shutdown and clean task cancellation.
CMD ["uvicorn", "app.main:app", \
     "--host", "0.0.0.0", \
     "--port", "8000", \
     "--workers", "1", \
     "--log-level", "info", \
     "--no-access-log"]
