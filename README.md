# Nimbus Freight — API

Spring Boot service for sustained reefer temperature alerts (`HIGH_TEMP` and `LOW_TEMP`). Pair with the [frontend repo](https://github.com/siddharth8763/nimbus-freight-frontend).

- [SOLUTION_DESIGN.md](SOLUTION_DESIGN.md) — assumptions and trade-offs (hackathon)
- [../DOCUMENTATION.md](../DOCUMENTATION.md) — full technical documentation

## Prerequisites

- Java 17+ (`JAVA_HOME` set to your JDK)
- Docker (MySQL 8)

## Run

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
```

API: http://localhost:8080 — replay seed with `POST /api/admin/replay-seed`.

## Configuration

Thresholds and seed path: `src/main/resources/application.yml` (`nimbus.alert.*`, `nimbus.seed-path`).

Optional alert summaries: set `LLM_API_KEY`. If that is empty, `OPENAI_API_KEY` is still accepted. The model only rewrites the sentence; it does not decide whether to alert.

OpenAI (default):

```powershell
$env:LLM_API_KEY = "sk-..."
.\mvnw.cmd spring-boot:run
```

DeepSeek (OpenAI-compatible chat API):

```powershell
$env:LLM_API_KEY = "your-deepseek-key"
$env:SPRING_PROFILES_ACTIVE = "deepseek"
.\mvnw.cmd spring-boot:run
```

That profile sets `base-url` to `https://api.deepseek.com/v1` and `model` to `deepseek-chat`. With H2 local storage, use `dev,deepseek`. See `.env.example`. If the call fails, the fixed template sentence is stored.

## Tests

```powershell
.\mvnw.cmd test
```

## Seed data

`src/main/resources/seed/sensor_readings.json` — the starter-kit hour for trucks T-01 through T-05. Replay sorts it, then uses the same ingest path as a live reading. Humidity is ignored.
