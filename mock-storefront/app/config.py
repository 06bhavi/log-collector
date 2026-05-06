"""
config.py — Centralised settings loaded from environment variables / .env file.

All tuneable knobs live here so that nothing is hard-coded in application logic.
"""

from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Application settings resolved from env vars (overrides .env file)."""

    # ── Log-collector target ────────────────────────────────────────────────
    log_collector_url: str = "http://log-collector:8080/api/logs"

    # ── Event-generator timing ──────────────────────────────────────────────
    emit_interval_seconds: float = 2.0

    # ── HTTP client behaviour ───────────────────────────────────────────────
    http_timeout_seconds: float = 5.0
    http_retry_attempts: int = 3

    # ── Uvicorn / service metadata ─────────────────────────────────────────
    app_title: str = "mock-storefront"
    app_version: str = "1.0.0"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
    )


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    """Return a cached singleton Settings instance."""
    return Settings()
