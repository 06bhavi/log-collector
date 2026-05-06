"""
log_sender.py — Async background task that continuously emits e-commerce events.

Design:
  - Runs as a persistent asyncio loop alongside the FastAPI application.
  - Uses httpx.AsyncClient with connection pooling (one client for the
    lifetime of the process — no socket churn).
  - Implements exponential back-off retries so transient log-collector
    downtime doesn't crash the generator.
  - Errors are logged and swallowed; the loop always continues.
"""

import asyncio
import logging

import httpx

from app.config import get_settings
from app.event_generator import generate_event
from app.schemas import UserEvent

log = logging.getLogger(__name__)

# ── Module-level shared HTTP client (initialised at startup) ──────────────────
_http_client: httpx.AsyncClient | None = None


def get_http_client() -> httpx.AsyncClient:
    """Return the module-level shared async HTTP client."""
    if _http_client is None:
        raise RuntimeError("HTTP client has not been initialised. Call init_client() first.")
    return _http_client


async def init_client() -> None:
    """Create the shared AsyncClient. Called once at application startup."""
    global _http_client
    settings = get_settings()
    _http_client = httpx.AsyncClient(
        timeout=httpx.Timeout(settings.http_timeout_seconds),
        headers={"Content-Type": "application/json", "Accept": "application/json"},
        # Keep connections alive to avoid per-request TCP handshake overhead
        limits=httpx.Limits(max_keepalive_connections=5, max_connections=10),
    )
    log.info("HTTP client initialised (target: %s)", settings.log_collector_url)


async def close_client() -> None:
    """Gracefully close the shared AsyncClient. Called at application shutdown."""
    global _http_client
    if _http_client is not None:
        await _http_client.aclose()
        _http_client = None
        log.info("HTTP client closed.")


# ── Core send helper ──────────────────────────────────────────────────────────

async def _send_event(event: UserEvent, attempt: int = 1) -> bool:
    """
    POST a single event to the log-collector with exponential back-off retry.

    Args:
        event:   The UserEvent to transmit.
        attempt: Current attempt number (used for back-off calculation).

    Returns:
        True on success, False after all retries are exhausted.
    """
    settings = get_settings()
    client   = get_http_client()

    try:
        response = await client.post(
            settings.log_collector_url,
            content=event.model_dump_json(),
        )
        response.raise_for_status()
        log.info(
            "✅ Sent  | user=%-10s action=%-20s product=%s → %s",
            event.userId,
            event.action,
            event.productId or "—",
            response.status_code,
        )
        return True

    except httpx.HTTPStatusError as exc:
        log.warning(
            "⚠️  HTTP %s from log-collector (attempt %d/%d): %s",
            exc.response.status_code,
            attempt,
            settings.http_retry_attempts,
            exc.response.text[:200],
        )
    except httpx.RequestError as exc:
        log.warning(
            "⚠️  Network error (attempt %d/%d): %s",
            attempt,
            settings.http_retry_attempts,
            exc,
        )

    # Retry with back-off if attempts remain
    if attempt < settings.http_retry_attempts:
        backoff = 2 ** (attempt - 1)          # 1 s, 2 s, 4 s …
        log.info("   Retrying in %ds …", backoff)
        await asyncio.sleep(backoff)
        return await _send_event(event, attempt + 1)

    log.error("❌ Giving up on event after %d attempts.", settings.http_retry_attempts)
    return False


# ── Background loop ───────────────────────────────────────────────────────────

async def run_event_loop() -> None:
    """
    Infinite async loop that generates and ships one event every
    `emit_interval_seconds`.  Designed to be launched via asyncio.create_task().
    """
    settings = get_settings()
    log.info(
        "🚀 Event loop started — emitting every %.1fs → %s",
        settings.emit_interval_seconds,
        settings.log_collector_url,
    )

    while True:
        event = generate_event()
        # Fire-and-forget: don't let a single failure stall the loop
        await _send_event(event)
        await asyncio.sleep(settings.emit_interval_seconds)
