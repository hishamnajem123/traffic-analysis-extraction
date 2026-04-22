# Traffic Analysis Extraction

Quarkus-based service responsible for ingesting traffic alerts from Waze, normalizing them into domain models, computing snapshot diffs, and publishing new events downstream.

---

## 🧠 Responsibilities

- Poll Waze Live Map API on a fixed schedule
- Normalize raw alert payloads into TrafficAlert domain objects
- Maintain an in-memory snapshot of active alerts
- Compute deltas (new / existing / removed alerts)
- Publish newly detected alerts for downstream processing

---

## 🏗️ Architecture Overview

PollScheduler  
    ↓  
ExtractionService  
    ↓  
WazeClient (REST client)  
    ↓  
WazeAlertMapper (DTO → Domain)  
    ↓  
SnapshotDiffService  
    ↓  
AlertPublisher  

---

## ⚙️ Tech Stack

- Java 21
- Quarkus
- Maven
- Micrometer (metrics)
- MapStruct (mapping)
- Quarkus REST Client

---

## 🚀 Running the application

### Dev mode

Run:

    ./mvnw quarkus:dev

Dev UI:

    http://localhost:8080/q/dev

---

## 📦 Build

Run:

    ./mvnw package

Then run the app:

    java -jar target/quarkus-app/quarkus-run.jar

---

## 🐳 Docker

Dockerfiles are located in:

    src/main/docker/

Example build:

    docker build -f src/main/docker/Dockerfile.jvm -t extraction .

---

## 📊 Metrics

Prometheus endpoint:

    http://localhost:8080/q/metrics

Includes:
- poll duration
- success / failure counts
- skipped polls

---

## 🔮 Future Work

- Redis-backed idempotency layer
- RabbitMQ integration for event streaming
- SMS notification service
- Multi-region ingestion
- Observability (tracing + dashboards)

---

## 📍 Notes

- Uses jittered bounding box to avoid API caching
- Designed as a single-responsibility ingestion microservice
- Intended to run as part of a distributed event-driven system

