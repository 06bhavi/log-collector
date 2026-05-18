"""
main.py — FastAPI application entry-point for mock-storefront.

Lifecycle:
  startup  → initialise shared HTTP client → launch background event loop
  shutdown → cancel background task      → close HTTP client gracefully

Endpoints:
  GET  /healthz        – liveness probe (for Docker / k8s)
  GET  /api/status     – human-readable service status
  POST /api/events     – manually inject a single event (useful for testing)
  GET  /api/events/sample – preview a randomly generated event payload
"""

import asyncio
import logging
import sys
from contextlib import asynccontextmanager
from datetime import datetime, timezone

from fastapi import FastAPI, HTTPException, status
from fastapi.responses import JSONResponse, RedirectResponse

from app.config import get_settings
from app.event_generator import generate_event
from app.log_sender import close_client, init_client, run_event_loop, _send_event
from app.schemas import UserEvent

# ── Logging configuration ─────────────────────────────────────────────────────
logging.basicConfig(
    stream=sys.stdout,
    level=logging.INFO,
    format="%(asctime)s [%(levelname)-8s] %(name)s — %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger(__name__)

# ── Background task handle ────────────────────────────────────────────────────
_event_loop_task: asyncio.Task | None = None


# ── Application lifespan (replaces deprecated on_event decorators) ────────────

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Manage startup and shutdown of shared resources and background tasks."""
    global _event_loop_task
    settings = get_settings()

    log.info("═" * 60)
    log.info("  mock-storefront v%s starting …", settings.app_version)
    log.info("  Log collector  : %s", settings.log_collector_url)
    log.info("  Emit interval  : %.1fs", settings.emit_interval_seconds)
    log.info("═" * 60)

    # Initialise shared HTTP client
    await init_client()

    # Launch the background event generator
    _event_loop_task = asyncio.create_task(
        run_event_loop(), name="event-generator"
    )

    yield  # ── Application running ──────────────────────────────────────────

    # Cancel background task gracefully
    if _event_loop_task and not _event_loop_task.done():
        _event_loop_task.cancel()
        try:
            await _event_loop_task
        except asyncio.CancelledError:
            log.info("Background event loop cancelled.")

    await close_client()
    log.info("mock-storefront shut down cleanly.")


# ── FastAPI application ───────────────────────────────────────────────────────

settings = get_settings()

app = FastAPI(
    title=settings.app_title,
    version=settings.app_version,
    description=(
        "Mock e-commerce storefront that continuously generates realistic "
        "user events and forwards them to the log-collector service."
    ),
    docs_url="/docs",
    redoc_url="/redoc",
    lifespan=lifespan,
)


# ── Routes ────────────────────────────────────────────────────────────────────

@app.get("/", include_in_schema=False)
async def root():
    return RedirectResponse(url="/docs")

@app.get(
    "/healthz",
    tags=["Operations"],
    summary="Liveness probe",
    response_description="Always returns 200 OK when the process is alive",
)
async def health_check() -> JSONResponse:
    """Docker / Kubernetes liveness probe endpoint."""
    return JSONResponse({"status": "ok"})


@app.get(
    "/api/status",
    tags=["Operations"],
    summary="Service status",
)
async def service_status() -> dict:
    """Return human-readable runtime status of the event-generator loop."""
    cfg = get_settings()
    task_running = (
        _event_loop_task is not None
        and not _event_loop_task.done()
        and not _event_loop_task.cancelled()
    )
    return {
        "service":        "mock-storefront",
        "version":        cfg.app_version,
        "event_loop":     "running" if task_running else "stopped",
        "target":         cfg.log_collector_url,
        "emit_interval":  f"{cfg.emit_interval_seconds}s",
        "server_time":    datetime.now(timezone.utc).isoformat(),
    }


@app.get(
    "/api/events/sample",
    tags=["Events"],
    summary="Preview a randomly generated event",
    response_model=UserEvent,
)
async def sample_event() -> UserEvent:
    """Generate and return a sample event without sending it to log-collector."""
    return generate_event()


@app.post(
    "/api/events",
    tags=["Events"],
    summary="Manually inject an event",
    status_code=status.HTTP_202_ACCEPTED,
)
async def inject_event(event: UserEvent) -> dict:
    """
    Accept a custom UserEvent payload and forward it immediately
    to the log-collector service.  Useful for ad-hoc testing.
    """
    success = await _send_event(event)
    if not success:
        raise HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail="Failed to forward event to log-collector after retries.",
        )
    return {
        "status":     "forwarded",
        "event":      event.model_dump(),
        "forwarded_to": get_settings().log_collector_url,
    }
