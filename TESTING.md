# Testing Guide

## Backend

```bash
cd server
$env:JWT_SECRET='local-test-secret-key-must-be-at-least-32-characters'
$env:SPRING_DATASOURCE_PASSWORD='password'
$env:SPRING_RABBITMQ_PASSWORD='guest'
mvn test
```

Test groups:

- `OrderServiceTest`: state transitions, idempotency, stock reservation, payment handling
- `OrderOutboxServiceTest`: durable outbox event creation and serialized payload shape
- `OutboxEventRelayTest`: RabbitMQ relay publishing, retry state, and dead-letter cutoff behavior
- `ProductServiceTest`: product CRUD service paths and missing-resource handling
- `OrderControllerTest`: REST validation, auth requirement, paged order listing, payment/status endpoints
- `ProductOptimisticLockingTest`: JPA `@Version` conflict behavior
- `OrderRepositoryPostgresContainerTest`: PostgreSQL persistence with Testcontainers when Docker is available

If Docker Desktop is not running or not exposing a valid Docker engine, the Testcontainers PostgreSQL test is skipped.

## Frontend

```bash
cd client
npm ci
npm run lint
npm run build
```

## Manual Smoke Test

```bash
docker compose --env-file .env.example up --build
```

Then verify:

- Login at http://localhost:5173 with `user/user12345`
- Create an order from the dashboard
- Capture payment and observe status progression toward `SHIPPED`
- Check that paid-order messages pass through the outbox relay before RabbitMQ consumption
- Login as `manager/manager123` and review operations
- Login as `admin/admin123` and add an inventory item
- Open Swagger at http://localhost:8080/swagger-ui.html
- Open RabbitMQ at http://localhost:15672
