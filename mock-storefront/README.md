# mock-storefront — E-Commerce Event Generator

A **FastAPI** microservice that continuously generates realistic e-commerce
user events and forwards them to the `log-collector` service every **2 seconds**.

---

## Project Structure

```
mock-storefront/
├── Dockerfile
├── .dockerignore
├── .env.example
├── requirements.txt
└── app/
    ├── __init__.py
    ├── main.py              ← FastAPI app + lifespan management
    ├── config.py            ← Pydantic settings (env-var driven)
    ├── schemas.py           ← UserEvent / EventAcknowledgement models
    ├── event_generator.py   ← Weighted random event factory
    └── log_sender.py        ← Async HTTP client + retry logic
```

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET`  | `/healthz` | Liveness probe (Docker / k8s) |
| `GET`  | `/api/status` | Runtime status of event-generator loop |
| `GET`  | `/api/events/sample` | Preview a random event without sending it |
| `POST` | `/api/events` | Manually inject a custom event |
| `GET`  | `/docs` | Swagger UI |

---

## Configuration

All settings are controlled via environment variables (or a `.env` file):

| Variable | Default | Description |
|---|---|---|
| `LOG_COLLECTOR_URL` | `http://log-collector:8080/api/logs` | Target endpoint |
| `EMIT_INTERVAL_SECONDS` | `2.0` | Seconds between events |
| `HTTP_TIMEOUT_SECONDS` | `5.0` | Per-request timeout |
| `HTTP_RETRY_ATTEMPTS` | `3` | Retry count on failure |

---

## Build & Run

### Local (bare Python)
```bash
# Create and activate virtual environment
python -m venv .venv
.venv\Scripts\activate          # Windows
# source .venv/bin/activate     # Linux/macOS

# Install dependencies
pip install -r requirements.txt

# Copy and edit env file
copy .env.example .env

# Run
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### Docker (standalone)
```bash
docker build -t mock-storefront:latest .

docker run -p 8000:8000 \
  -e LOG_COLLECTOR_URL=http://host.docker.internal:8080/api/logs \
  mock-storefront:latest
```

### Docker Compose (with log-collector)
```bash
# From the project root containing docker-compose.yml:
docker compose up --build
```

---

## Generated Events

Events are weighted to simulate a realistic purchase funnel:

| Action | Weight | Notes |
|---|---|---|
| `item_viewed` | 45 | Most common |
| `search_performed` | 15 | |
| `add_to_cart` | 20 | |
| `checkout_started` | 10 | No productId |
| `purchase` | 8 | |
| `wishlist_add` | 6 | |
| `remove_from_cart` | 5 | |
| `coupon_applied` | 3 | Least common |

---

## Key Design Decisions

| Concern | Decision |
|---|---|
| Background task | `asyncio.create_task()` within FastAPI lifespan — no threads needed |
| HTTP client | Shared `httpx.AsyncClient` with keepalive — no per-request TCP overhead |
| Retry logic | Exponential back-off (1s, 2s, 4s) so transient failures don't spam logs |
| Graceful shutdown | Task cancellation + client close in lifespan teardown |
| Base image | `python:3.10-slim` — no dev tools, minimal attack surface |
| Security | Non-root user (`appuser`), no pip cache in image, exec-form CMD |
| Configurability | All knobs in environment variables, no hard-coded values |
