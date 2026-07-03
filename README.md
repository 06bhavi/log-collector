# E-Commerce Big Data Pipeline & Analytics Platform

This project is a complete, containerised, microservices-based big data pipeline and analytics platform for simulating and processing e-commerce logs. The system features an end-to-end flow: from generating mock e-commerce interactions, ingesting them via a Spring Boot REST API, persisting them to Hadoop Distributed File System (HDFS), and visualising the data through a React-based analytics dashboard.

The entire infrastructure is orchestrated with **Docker Compose** and automated with comprehensive **CI/CD** pipelines using **GitHub Actions** and **Jenkins**.

---

## 🏗 Architecture & Services

The platform consists of the following core components:

### 1. `mock-storefront` (Python / FastAPI)
A Python application that acts as an e-commerce simulator. It periodically generates mock user interactions, transactions, and logs, and sends them to the log collector via POST requests.
- **Framework:** FastAPI
- **Internal Port:** 8000

### 2. `log-collector` (Java / Spring Boot)
A robust Spring Boot backend application exposing a REST API (`/api/logs`). It receives the log entries from the storefront and acts as an ingestion layer, persisting the data directly into an HDFS cluster.
- **Framework:** Spring Boot (Java 17)
- **Internal Port:** 8080

### 3. Hadoop Cluster (NameNode & DataNode)
A two-node Apache Hadoop 3.2.1 cluster used for distributed storage. The NameNode manages the filesystem namespace and access, while the DataNode actually stores the e-commerce log data (`/logs/ecommerce_data.json`).
- **Ports:** 9870 (NameNode Web UI), 9000 (HDFS RPC)

### 4. `analytics-dashboard` (React / Nginx)
A modern React web frontend built with Vite. It consumes data and provides a visual dashboard to monitor and analyse the simulated e-commerce metrics. The production build is served via Nginx.
- **Framework:** React (Vite) + Nginx
- **Exposed Port:** 3001

---

## 🚀 Getting Started (Local Development)

### Prerequisites
- [Docker](https://www.docker.com/) & [Docker Compose](https://docs.docker.com/compose/)
- Git

### Running the Project

1. **Clone the repository:**
   ```bash
   git clone https://github.com/06bhavi/log-collector.git
   cd log-collector
   ```

2. **Start the infrastructure:**
   ```bash
   docker-compose up -d --build
   ```

3. **Access the Services:**
   - **Analytics Dashboard:** [http://localhost:3001](http://localhost:3001)
   - **Hadoop NameNode UI:** [http://localhost:9870](http://localhost:9870)
   - **Log Collector API:** [http://localhost:8080/api/logs](http://localhost:8080/api/logs)
   - **Mock Storefront Docs:** [http://localhost:8000/docs](http://localhost:8000/docs)

4. **Stop the infrastructure:**
   ```bash
   docker-compose down
   ```
   *(Note: HDFS data is persisted in Docker named volumes)*

---

## 🔄 CI/CD Automation

This repository embraces automated DevOps practices, utilising both GitHub Actions for CI and Jenkins for CD.

### Continuous Integration (GitHub Actions)
Triggered on pushes to the `main` branch, the workflow (`.github/workflows/ci.yml`) executes the following jobs:
- **Linting:** Runs `flake8` against the Python `mock-storefront` code.
- **Build & Test:** Compiles and runs unit tests for the Java `log-collector` using Maven.
- **Docker Image Build & Push:** If tests pass, it uses Docker Buildx to build all three custom images (`log-collector`, `mock-storefront`, `analytics-dashboard`) and pushes them to GitHub Container Registry (GHCR) with both `latest` and commit SHA tags.

### Continuous Deployment (Jenkins)
A Jenkins pipeline (`Jenkinsfile`) is configured to:
- Authenticate and pull the freshly built images from GHCR.
- Re-tag the remote images for local use.
- Execute `docker-compose down` followed by `docker-compose up -d --no-build` to restart the services with the newest application versions without manual intervention.

---

## 📂 Directory Structure

```
.
├── .github/workflows/    # GitHub Actions CI definitions
├── analytics-dashboard/  # React Vite frontend application & Nginx Dockerfile
├── log-collector/        # Spring Boot Java ingestion service
├── mock-storefront/      # Python FastAPI data generator
├── docker-compose.yml    # Master docker-compose orchestration
├── hadoop.env            # Environment variables for the Hadoop cluster
├── Jenkinsfile           # Jenkins Continuous Deployment pipeline
└── README.md             # This project documentation
```
