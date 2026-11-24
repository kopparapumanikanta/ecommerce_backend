# E-commerce Backend (learning project, retail flavor)

Spring Boot 3.5 / Java 21 backend with a React frontend (`ecommerce-frontend`). Built to practice a full-stack retail stack: products, inventory, orders, events.

## Architecture

- **API**: Controller → Service → Repository. REST + JSON, bean validation, central error handler (`GlobalExceptionHandler`).
- **Auth**: `POST /auth/login` returns a signed JWT (HS256). Roles `ADMIN` and `STORE_USER`. Stateless Spring Security resource server. Passwords hashed with BCrypt.
- **PostgreSQL (JPA)**: products, users, orders, order_items. Order placement is one `@Transactional` unit with a pessimistic row lock on stock, so two orders cannot oversell.
- **Redis**: cache-aside for `GET /products`, evicted on writes.
- **Kafka**: `product-created` and `order-created` events (JSON with `eventId`). Events are published after the DB commit. Consumer retries 3 times (1s apart), then the message goes to `<topic>.DLT`. Bad JSON skips retries.
- **DynamoDB**: event log keyed by `eventId`. A conditional write (`attribute_not_exists`) makes the consumer idempotent, so a redelivered event is skipped. GSI on `entityId` powers `GET /products/{id}/events`.
- **Observability**: Actuator health, Prometheus metrics (`/actuator/prometheus`), custom counters (`orders.placed`, `events.processed`, `events.duplicate`, `events.dead_lettered`), request id in every log line (`X-Request-Id`), JSON logs with `SPRING_PROFILES_ACTIVE=json`.
- **CI/CD**: GitHub Actions runs tests on every push/PR and pushes a Docker image to GHCR on `main`; a manual workflow deploys to AWS ECS.
- **IaC**: `infra/terraform` (ECR, CloudWatch log group, SQS + DLQ, DynamoDB table).

Known gap (good interview answer): Postgres and Kafka cannot share one transaction. Publishing after commit can lose an event if the app crashes in between. The production fix is the transactional outbox pattern.

## Endpoints

| Method | Path | Who |
|---|---|---|
| GET | /hello | public |
| POST | /auth/login | public |
| GET | /products, /products/{id}, /products/low-stock?threshold=5 | any signed-in user |
| GET | /products/{id}/events | any signed-in user |
| POST | /products | ADMIN |
| POST | /orders | any signed-in user |
| GET | /orders/mine | any signed-in user |
| GET | /orders/reports/sales | ADMIN |

Seeded users: `admin` (ADMIN) and `store1` (STORE_USER). Passwords come from `ADMIN_PASSWORD` / `STORE_PASSWORD` (see `.env.example`; local defaults are in `application.yaml`).

## Run everything with Docker Compose

```bash
cp .env.example .env        # then edit the values
docker compose up --build
```

Try it:

```bash
TOKEN=$(curl -s -X POST localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"<ADMIN_PASSWORD>"}' | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")
curl -s localhost:8080/products -H "Authorization: Bearer $TOKEN"
curl -s -X POST localhost:8080/orders -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"items":[{"productId":1,"quantity":2}]}'
curl -s localhost:8080/orders/reports/sales -H "Authorization: Bearer $TOKEN"
curl -s localhost:8080/actuator/health
```

## Run locally from IntelliJ

Start only the infrastructure, then run the app from IntelliJ:

```bash
docker compose up postgres redis kafka dynamodb
```

IntelliJ run config environment variables: `DB_USERNAME=ecommerce;DB_PASSWORD=<same as .env>;SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ecommerce_db`. Kafka's advertised address is `kafka:9092`, so for IntelliJ either add `127.0.0.1 kafka` to `/etc/hosts` or run the app via compose.

Frontend: `cd ../Downloads/ecommerce-frontend && npm install && npm run dev` (http://localhost:5173).

## Tests

```bash
./mvnw clean verify
```

Unit and web-slice tests need no external services. The full-context test runs only with `RUN_INTEGRATION_TESTS=true` and live services.

## Interview talking points

- Why Redis cache-aside, and what must be evicted on writes.
- Why `PESSIMISTIC_WRITE` on stock rows (and optimistic locking as the alternative).
- Why publish after commit, and what the outbox pattern fixes.
- Idempotent consumers: at-least-once delivery means duplicates happen; the conditional put handles them.
- Retries vs dead-letter: transient errors retry, poison messages go to the DLT for review.
- JWT: stateless, signed, short-lived; roles in a claim; secrets come from env / Secrets Manager, never from code.
- SQL: the sales report is a JPQL join + group by; `idx_orders_customer` speeds up "my orders".
- Honest framing: a learning project that mirrors production patterns, not production experience.

## Deploy to AWS (EC2 + S3)

Terraform (`infra/terraform`) creates a t3.medium EC2 instance with an Elastic IP, an IAM role (DynamoDB access + Session Manager), a security group that opens only port 8080, and a public S3 bucket for the React build. The server runs the same Docker Compose stack, using `docker-compose.ec2.yml`. Connect with Session Manager (no SSH keys). Remove everything with `terraform destroy`.

## Secrets and monitoring on AWS

- Secrets live in AWS Secrets Manager (`ecommerce/app`, one JSON secret). On the server, `scripts/fetch-secrets.sh` writes `.env` from it using the instance role, so no secret is stored in git or in Terraform.
- The app container sends JSON logs (with the request id) to CloudWatch Logs (`/ecs/ecommerce-backend`). A metric filter counts `ERROR` lines, and an alarm emails you through SNS. A second alarm watches the EC2 status check.
