# Fuel Service — FleetOps (G08)

![CI](https://github.com/FleetOpsProyect/grupo_08_combustible/actions/workflows/ci.yml/badge.svg?branch=develop)
![Coverage](https://img.shields.io/badge/coverage-99%25-brightgreen)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-brightgreen?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker)
![Architecture](https://img.shields.io/badge/architecture-hexagonal-informational)

Fuel microservice of **FleetOps**, the fleet management backend of FleetCorp S.A. (Desarrollo de Software III, Univalle). It handles four things:
- **Fuel records:** registers every refueling made during a route in progress.
- **Efficiency:** calculates km per gallon on every full tank.
- **Anomalous consumption:** flags a refueling when its efficiency drops more than 30 % below the vehicle average.
- **Reports:** gives Reports (G10) the cost summary.


---

## Tech stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java (Temurin) | 21 LTS |
| Framework | Spring Boot (Web, Validation, Data JPA, Actuator) | 3.5.16 |
| Database | PostgreSQL (one database per service) | 16.15 |
| Migrations | Flyway | managed by Spring Boot |
| HTTP clients | Spring RestClient | managed by Spring Boot |
| Resilience | Resilience4j Circuit Breaker + 2 s timeout | 2.4.0 |
| API docs | springdoc OpenAPI + Scalar | 2.8.17 |
| Metrics | Micrometer + Prometheus registry | managed by Spring Boot |
| Monitoring | Prometheus + Grafana (provisioned dashboard) | 3.13.4 / 13.0.11 |
| Tests | JUnit 5, Mockito, MockMvc, Testcontainers | managed by Spring Boot |
| Coverage | JaCoCo | 0.8.15 |
| Static analysis | Checkstyle (local) + SonarCloud (CI) | 10.26.1 / cloud |
| PR review | Qodo PR-Agent with Gemini (Google AI Studio) | 0.47.0 |
| Build | Maven Wrapper | 3.9.16 |
| Containers | Docker + Docker Compose | — |
| Image registry | Docker Hub | — |
| CI/CD | GitHub Actions | — |
| Hosting | AWS EC2 (t3.micro, free tier) | — |

---

## Architecture

The service uses a **hexagonal architecture** (ports and adapters). The core (domain and use cases) is plain Java and does not know Spring Web, JPA, HTTP or the broker. Everything outside the core goes through a port, and **dependencies always point to the center**.

```
                 ┌──────────────────── Application core ────────────────────┐
 REST request ─▶ │ Inbound   ─▶ Use cases ─▶ Domain                         │
 Scheduler    ─▶ │ ports          │                                         │
                 │                └──────▶ Outbound ports                   │
                 └──────────────────────────────┬───────────────────────────┘
                                                ▼
                     Outbound adapters: PostgreSQL · Assignments (G2) · Routes (G6) · Event broker
```

| Layer | Package | Role |
|-------|---------|------|
| Domain | `.domain` | `FuelRecord`, `EfficiencyCalculator`, `AnomalyRule` (Strategy), `VehicleFuelAverage`, `FuelAnomalyDetected` |
| Inbound ports | `.application.port.in` | What the service offers: register, query, relay outbox |
| Outbound ports | `.application.port.out` | What the service needs: assignments, routes, repositories, outbox, event publisher |
| Use cases | `.application.usecase` | Orchestrate the flow; one transaction per registration |
| Inbound adapters | `.adapter.in` | `FuelRecordController` (REST) and `OutboxRelayScheduler` |
| Cross-cutting | `.crosscutting` | `CorrelationIdFilter`, `RequestValidator`, `GlobalExceptionHandler` |
| Outbound adapters | `.adapter.out` | REST clients with Circuit Breaker, JPA adapters, event publisher |
| Utilities | `.utilities` | `AppConfig`, `ClockProvider` (America/Bogota), `BeanConfiguration`, `OpenApiDocs` (Scalar) |

Key decisions, from the G08 Software Architecture Document:
- **Synchronous calls to G2 and G6:** 2 s timeout and Circuit Breaker. If either fails, the service answers 503 and stores nothing.
- **Transactional Outbox:** the anomaly event is saved in the same transaction as the fuel record, and a scheduler publishes it later.
- **Anomaly rule:** an anomaly is a drop of more than 30 % against the vehicle average. It needs at least 2 previous efficiencies, and anomalous values never enter the average. Both values are configurable.
- **Contract:** English and snake_case, dates in GMT-5, and a common error body (`code`, `message`, `timestamp`, `path`, `errors`).

---

## Environments

| Environment | URL | Trigger |
|-------------|-----|---------|
| Local | `http://localhost:8088` | `docker compose up` |
| AWS EC2 | `http://<EC2_IP>:<APP_PORT>` | Continuous deployment on every push to `main` |
| Prometheus | `http://localhost:9090` (EC2: `<EC2_IP>:<PROMETHEUS_PORT>`) | Started with the service |
| Grafana | `http://localhost:3000` (EC2: `<EC2_IP>:<GRAFANA_PORT>`) | Dashboard *Fuel Service (G08)*, user from `.env` |

---

## Quick start

### Local (Docker)

```bash
git clone https://github.com/FleetOpsProyect/grupo_08_combustible.git && cd grupo_08_combustible

cp .env.example .env
# Every variable is documented inside .env.example

docker compose up --build
```

| Service | URL |
|---------|-----|
| API | http://localhost:8088/fuel_records |
| API reference (Scalar) | http://localhost:8088/scalar |
| OpenAPI JSON | http://localhost:8088/v3/api-docs |
| Health | http://localhost:8088/health |
| Prometheus metrics | http://localhost:8088/prometheus |
| Prometheus | http://localhost:9090 |
| Grafana (dashboard *Fuel Service (G08)*) | http://localhost:3000 |

> **Note:** Assignments (G2) and Telemetry and Routes (G6) are not deployed yet. Docker Compose starts a **WireMock** container that simulates them; when the real services exist, only `ASSIGNMENTS_BASE_URL` and `ROUTES_BASE_URL` change. The event broker is not chosen yet either, so the anomaly event is written to the log.

Test data simulated by WireMock:

| `assignment_id` | Result |
|-----------------|--------|
| `a1111111-1111-4111-8111-111111111111` | 201: assignment and route in progress |
| `a2222222-2222-4222-8222-222222222222` | 409 `ASSIGNMENT_NOT_IN_PROGRESS` |
| `a5555555-5555-4555-8555-555555555555` | 409 `ROUTE_NOT_IN_PROGRESS` |
| `a4444444-4444-4444-8444-444444444444` | 503 `DEPENDENCY_UNAVAILABLE` (G2 answers after 3 s) |
| any other id | 404 `ASSIGNMENT_NOT_FOUND` |

To start again with an empty database: `docker compose down -v`.

### From IntelliJ IDEA (service running natively)

The service reads the `.env` file of the project root by itself, so the IDE needs no extra configuration. The default values of `.env.example` point to `localhost`.

1. Open the project folder in IntelliJ. It is detected as a Maven project; set the **Project SDK to Java 21**.
2. Copy the configuration: `cp .env.example .env`.
3. Start the database and the G2/G6 simulator. Choose one option:
   - **With Docker (only the dependencies):**
     ```bash
     docker compose up -d fuel-db dependencies-mock
     ```
     PostgreSQL is published on port **5433** (so it does not clash with a native PostgreSQL on 5432) and WireMock on **8089**.
   - **Without Docker:**
     - Use a native PostgreSQL 16 and create the database:
       ```sql
       CREATE USER fuel_user WITH PASSWORD 'change_me_local_password';
       CREATE DATABASE fleetops_fuel OWNER fuel_user;
       ```
       Then set `DB_URL=jdbc:postgresql://localhost:5432/fleetops_fuel` in `.env`.
     - Run WireMock as a plain Java process with the same stubs:
       ```bash
       curl -LO https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/3.13.2/wiremock-standalone-3.13.2.jar
       java -jar wiremock-standalone-3.13.2.jar --port 8089 --root-dir wiremock
       ```
4. Run `FuelApplication` (green arrow next to `main`). Flyway creates the tables on startup.

| Service | URL |
|---------|-----|
| API | http://localhost:8080/fuel_records |
| API reference (Scalar) | http://localhost:8080/scalar |
| Health | http://localhost:8080/health |

> Unit tests run from IntelliJ without Docker. The persistence integration test (Testcontainers) is skipped automatically when Docker is not running.

### Tests and coverage

```bash
./mvnw test -Dcheckstyle.skip   # Unit tests (domain and use cases use mocks, no DB)
./mvnw verify                   # Unit + integration tests, Checkstyle and JaCoCo gates
```

On Windows PowerShell use `.\mvnw.cmd` instead of `./mvnw`.

- **Coverage gates:** the build fails if `domain` or `application` drop below **100 %** of lines and branches, or if the whole service drops below **80 %**.
- **Coverage report:** `target/site/jacoco/index.html`.
- **Docker requirement:** the persistence adapters are tested against a real PostgreSQL 16 with Testcontainers, so Docker must be running.

---

## API endpoints

| Method | Path | Role | Responses |
|--------|------|------|-----------|
| POST | `/fuel_records` | Driver | 201, 400, 404, 409, 422, 503 |
| GET | `/fuel_records/{id}` | Fleet Administrator, internal services | 200, 400, 404 |
| GET | `/fuel_records` | Fleet Administrator, Reports (G10) | 200 with `summary`, 400 |
| GET | `/health` | Internal (no authentication) | 200, 503 |
| GET | `/prometheus` | Prometheus | 200 |

`GET /fuel_records` filters: `vehicle_id`, `assignment_id`, `route_id`, `anomalous_consumption`, `date_from`, `date_to`, `page` (from 0) and `size` (1 to 100).

Request body of `POST /fuel_records`:

```json
{
  "assignment_id": "a1111111-1111-4111-8111-111111111111",
  "gallons_purchased": 10.5,
  "total_cost": 157500.00,
  "odometer_km": 45210.0,
  "full_tank": true
}
```

**Published event:** `fuel_anomaly_detected_event`.
- It uses the shared envelope: `event_id`, `event_type`, `version`, `occurred_at`, `source`, `data`.
- Notifications (G4) consumes it.

> **Note:** authentication and roles are handled by the AWS API Gateway with Cognito, before the request reaches the service. Checking that a driver only registers fuel on their own assignment (403) belongs to phase 2 of the shared conventions.

---

## CI pipeline

`.github/workflows/ci.yml` runs on every pull request and push to `develop`:

1. Unit and integration tests
2. Coverage gate (JaCoCo)
3. Static analysis (Checkstyle)
4. Build of the deployable jar
5. Build of the Docker image
6. **SonarCloud** analysis, waiting for the Quality Gate
7. **Qodo** review, only on pull requests to `develop`

SonarCloud and Qodo run only on `develop`, as asked in class, so the monthly Qodo reviews are not spent on every developer push. SonarCloud uses **CI-based analysis**: the `sonarcloud` job analyzes every pull request and push to `develop` and fails if the Quality Gate does not pass. Automatic analysis must stay off (*Administration → Analysis method*), because SonarCloud rejects a CI analysis while the automatic one is on.

## CD pipeline

`.github/workflows/cd.yml` runs on every push to `main` (a release merged from `develop`):

1. Builds the image and pushes it to Docker Hub as `latest` and with the commit hash
2. Copies `docker-compose.prod.yml` and the WireMock stubs to the EC2 instance
3. Runs `docker compose pull` and `docker compose up -d` on the instance

**One-time setup of the EC2 instance** (Amazon Linux 2023, t3.micro, ports `APP_PORT` and `GRAFANA_PORT` open in the security group):

```bash
sudo dnf install -y docker && sudo systemctl enable --now docker && sudo usermod -aG docker ec2-user
sudo mkdir -p /usr/libexec/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/download/v2.39.4/docker-compose-linux-x86_64 -o /usr/libexec/docker/cli-plugins/docker-compose
sudo chmod +x /usr/libexec/docker/cli-plugins/docker-compose
# 2 GB of swap: the t3.micro has 1 GB of RAM for the service, WireMock, Prometheus and Grafana (the database is on Neon)
sudo dd if=/dev/zero of=/swapfile bs=1M count=2048 && sudo chmod 600 /swapfile && sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile swap swap defaults 0 0' | sudo tee -a /etc/fstab
mkdir -p ~/fleetops-fuel-service   # then create ~/fleetops-fuel-service/.env from .env.example
```

The instance must be running for the deployment to work.

## Secrets and variables

Configure in *Settings → Secrets and variables → Actions*:

| Type | Name | Purpose |
|------|------|---------|
| Secret | `SONAR_TOKEN` | SonarCloud token |
| Variable | `SONAR_PROJECT_KEY` | `FleetOpsProyect_grupo_08_combustible` |
| Variable | `SONAR_ORGANIZATION` | `fleetopsproyect-1` |
| Secret | `GEMINI_API_KEY` | Google AI Studio key used by Qodo PR-Agent |
| Variable | `QODO_ENABLED` | `true` to run the Qodo review |
| Secret | `DOCKERHUB_USERNAME` | Docker Hub user that owns the image |
| Secret | `DOCKERHUB_TOKEN` | Docker Hub access token (read and write) |
| Secret | `EC2_HOST` | Public IP or DNS of the EC2 instance |
| Secret | `EC2_USER` | SSH user of the instance (`ec2-user`) |
| Secret | `EC2_SSH_KEY` | Private key (`.pem`) of the instance |
| Variable | `CD_ENABLED` | `true` to run the deployment |

**Branching:**
- `main` holds the released version and `develop` integrates the team; both are protected.
- Each developer works on their own branch and merges into `develop` through a pull request with one approval and a green pipeline.

> On Windows, mark the wrapper as executable before the first commit: `git update-index --chmod=+x mvnw`.

---

## Project structure

```
fleetops-fuel-service/
├── .env.example                ← Every environment variable, documented
├── docker-compose.yml          ← PostgreSQL 16 + WireMock (G2, G6) + the service
├── docker-compose.prod.yml     ← Same stack on EC2, using the image from Docker Hub
├── Dockerfile                  ← Multi-stage build, non-root runtime user
├── checkstyle.xml              ← Local static analysis rules
├── pom.xml
├── .github/workflows/ci.yml    ← CI: tests, coverage, Checkstyle, build, SonarCloud, Qodo
├── .github/workflows/cd.yml    ← CD: Docker Hub + deployment to AWS EC2
├── wiremock/mappings/          ← Simulated responses of Assignments (G2) and Routes (G6)
├── monitoring/                 ← Prometheus scrape config, Grafana datasource and dashboard
└── src/
    ├── main/java/co/edu/univalle/fleetops/fuel/
    │   ├── domain/             ← Business rules, plain Java
    │   ├── application/
    │   │   ├── port/in/        ← Inbound ports (use case interfaces)
    │   │   ├── port/out/       ← Outbound ports
    │   │   ├── usecase/        ← Use case implementations
    │   │   ├── query/          ← Filters, page and summary
    │   │   └── exception/
    │   ├── adapter/
    │   │   ├── in/rest/        ← FuelRecordController + DTOs
    │   │   ├── in/scheduler/   ← OutboxRelayScheduler
    │   │   ├── out/rest/       ← Assignments and Routes clients + Resilience4j
    │   │   ├── out/persistence/← JPA entities, repositories and adapters
    │   │   └── out/event/      ← Event publisher (log until the broker is chosen)
    │   ├── crosscutting/       ← Correlation id, validation, error handling
    │   └── utilities/          ← Configuration, clock, wiring, API docs
    ├── main/resources/
    │   ├── application.yml
    │   └── db/migration/       ← Flyway: fuel_records, vehicle_fuel_averages, outbox_events
    └── test/                   ← Unit tests (Mockito), MockMvc, Testcontainers
```
