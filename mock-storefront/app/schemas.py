
from datetime import datetime, timezone
from typing import Optional
from pydantic import BaseModel, Field


# ── Allowed event action types ────────────────────────────────────────────────
VALID_ACTIONS = (
    "item_viewed",
    "add_to_cart",
    "remove_from_cart",
    "checkout_started",
    "purchase",
    "wishlist_add",
    "search_performed",
    "coupon_applied",
)


class UserEvent(BaseModel):
    """
    E-commerce user-event payload sent to the log-collector service.

    Mirrors the Java UserEvent model:
        userId    – unique user identifier
        action    – business event type
        timestamp – ISO-8601 UTC string (log-collector expects this format)
        productId – involved product (optional for non-product actions)
    """

    userId: str = Field(..., description="Unique user identifier")
    action: str = Field(..., description="Business event type")
    timestamp: str = Field(..., description="ISO-8601 UTC timestamp")
    productId: Optional[str] = Field(None, description="Product involved (if any)")

    @classmethod
    def utc_now_iso(cls) -> str:
        """Return current UTC time formatted exactly as log-collector expects."""
        return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")


class EventAcknowledgement(BaseModel):
    """Response body returned by the log-collector on success."""
    status: str
    message: str
    userId: str
    action: str
    receivedAt: str
