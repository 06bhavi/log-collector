"""
event_generator.py — Realistic e-commerce event factory.

Produces randomised but plausible UserEvent instances that mimic real
storefront behaviour:
  - A small pool of "users" keeps revisiting the shop
  - The action sequence follows a rough purchase funnel weight distribution
  - Products are drawn from a catalogue to simulate session continuity
"""

import random

from app.schemas import UserEvent

# ── Static data pools ─────────────────────────────────────────────────────────

# Simulate a recurring user base (not purely random UUIDs) so logs look real
_USER_POOL: list[str] = [f"user-{i:04d}" for i in range(1, 31)]

# Product catalogue (id → name for log readability)
_PRODUCT_CATALOGUE: list[dict] = [
    {"id": "prod-101", "name": "Wireless Earbuds Pro"},
    {"id": "prod-102", "name": "Mechanical Keyboard"},
    {"id": "prod-103", "name": "USB-C Hub 7-in-1"},
    {"id": "prod-104", "name": "4K Webcam"},
    {"id": "prod-105", "name": "Laptop Stand Aluminium"},
    {"id": "prod-106", "name": "Smart LED Desk Lamp"},
    {"id": "prod-107", "name": "Portable SSD 1TB"},
    {"id": "prod-108", "name": "Noise-Cancelling Headphones"},
    {"id": "prod-109", "name": "Ergonomic Mouse"},
    {"id": "prod-110", "name": "Vertical Monitor Stand"},
]

# Actions weighted to reflect a realistic purchase funnel:
#   Many views → some cart adds → fewer purchases
_ACTION_WEIGHTS: dict[str, int] = {
    "item_viewed": 45,
    "add_to_cart": 20,
    "remove_from_cart": 5,
    "checkout_started": 10,
    "purchase": 8,
    "wishlist_add": 6,
    "search_performed": 15,
    "coupon_applied": 3,
}

_ACTIONS, _WEIGHTS = zip(*_ACTION_WEIGHTS.items())


# ── Public API ────────────────────────────────────────────────────────────────


def generate_event() -> UserEvent:
    """
    Create a single randomised, realistic e-commerce UserEvent.

    Returns:
        UserEvent: A Pydantic model ready to be serialised and POSTed.
    """
    action = random.choices(_ACTIONS, weights=_WEIGHTS, k=1)[0]

    # Non-product actions (e.g. search_performed) don't need a productId
    product_id: str | None = None
    if action not in ("search_performed", "checkout_started"):
        product = random.choice(_PRODUCT_CATALOGUE)
        product_id = product["id"]

    return UserEvent(
        userId=random.choice(_USER_POOL),
        action=action,
        timestamp=UserEvent.utc_now_iso(),
        productId=product_id,
    )
