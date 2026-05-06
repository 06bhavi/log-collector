# ============================================================
#  Dockerfile — analytics-dashboard
#  Multi-stage build:
#    Stage 1 (builder) — Node 20 Alpine → npm run build → /app/dist
#    Stage 2 (runtime) — Nginx Alpine   → serve /app/dist on :80
#
#  Security hardening:
#   • Non-root nginx user (built in to the official image)
#   • Read-only nginx config from a custom nginx.conf
#   • No Node runtime in the final image — only static assets
# ============================================================

# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM node:20-alpine AS builder

LABEL stage="builder"

# Build-time argument lets CI/CD inject the real API URL, e.g.:
#   docker build --build-arg VITE_API_BASE_URL=http://log-collector:8080 .
ARG VITE_API_BASE_URL=http://log-collector:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

WORKDIR /app

# Copy dependency manifests first to leverage Docker layer caching:
# node_modules is only rebuilt when package*.json changes.
COPY package.json package-lock.json ./
RUN npm ci --silent

# Copy the rest of the source tree and build
COPY . .
RUN npm run build
# dist/ is now ready at /app/dist

# ── Stage 2: Runtime (Nginx) ─────────────────────────────────────────────────
FROM nginx:1.27-alpine AS runtime

# Remove the default Nginx config and replace with our hardened version
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/app.conf

# Copy compiled static assets from the builder stage
COPY --from=builder /app/dist /usr/share/nginx/html

# Nginx in the official Alpine image already drops to the 'nginx' user
# after binding port 80, so no explicit USER instruction is needed.

EXPOSE 80

# Use exec form so SIGTERM reaches nginx directly (graceful shutdown)
CMD ["nginx", "-g", "daemon off;"]
