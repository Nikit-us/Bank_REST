# Bank Card Management System

A REST service for issuing and managing bank cards, transferring money between a user's own
cards, and administering users. Authentication is stateless (JWT); two roles are supported,
`ADMIN` and `USER`.

## Tech stack

- **Java 21**, **Spring Boot 4.1** (Spring Framework 7, Spring Security 7, Hibernate 7)
- **PostgreSQL** with **Liquibase** for schema migrations
- **JWT** via jjwt; **OpenAPI/Swagger** via springdoc
- Lombok, MapStruct and Apache Commons Lang to keep boilerplate down
- Maven (wrapper included) and Docker Compose for builds and local runs

## Features

**Administrators** create users and cards, block/activate and delete cards, and can see every
card in the system.

**Users** operate only on their own cards: they browse them (with last-four search and
pagination), request a card to be blocked, transfer money between their cards, and check a
balance. Ownership is always enforced server-side, so one user can never reach another user's
card.

## Running the application

The quickest way is Docker Compose, which starts both the database and the application:

```bash
docker compose up --build
```

Once it is up, Swagger UI is available at <http://localhost:8080/swagger-ui.html>.

To run the application from your IDE instead, start only the database in Docker and launch the
app with Maven:

```bash
docker compose up -d db
./mvnw spring-boot:run
```

By default the application expects PostgreSQL on `localhost:5432` (database `bank_rest`,
username and password `bank`/`bank`). Every parameter can be overridden with environment
variables — see [Configuration](#configuration).

To stop everything and drop the database volume:

```bash
docker compose down -v
```

## Default accounts

These are seeded by the first Liquibase migration on startup. The passwords are for local
development only and must be changed for any real deployment.

| Username | Password     | Role  |
|----------|--------------|-------|
| `admin`  | `Admin12345` | ADMIN |
| `user`   | `User12345`  | USER  |
| `user2`  | `User12345`  | USER  |

## API documentation

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI (JSON): <http://localhost:8080/v3/api-docs>
- A checked-in copy of the specification: [`docs/openapi.yaml`](docs/openapi.yaml)

## Example requests

Authenticate and capture the token:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"Admin12345"}' | jq -r .token)
```

Issue a card for a user (admin only):

```bash
curl -X POST http://localhost:8080/api/v1/cards \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"cardNumber":"4111111111111111","ownerId":2,"expiryDate":"2030-12-31","initialBalance":1000.00}'
```

List your own cards with search and pagination:

```bash
curl "http://localhost:8080/api/v1/cards/my?search=1111&status=ACTIVE&page=0&size=10" \
  -H "Authorization: Bearer $USER_TOKEN"
```

Transfer money between your own cards:

```bash
curl -X POST http://localhost:8080/api/v1/cards/my/transfer \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"fromCardId":1,"toCardId":2,"amount":100.00}'
```

## Security

Card numbers are never stored in clear text. Each number is encrypted with AES-256-GCM using
Spring Security's `Encryptors.stronger` (random IV per value), and only a masked form —
`**** **** **** 1234` — is ever returned to clients. Uniqueness is enforced through a
deterministic HMAC of the number, so duplicates can be detected without decrypting anything.
Passwords are hashed with BCrypt.

Authorization is applied at two levels: URL rules in `SecurityConfig` and method-level checks
with `@PreAuthorize`. Errors are returned in a single consistent shape (`ErrorResponse`) with
appropriate status codes — `400` for validation, `401`/`403` for authentication and access,
`404`, `409` for conflicts, and `422` for business-rule violations.

## Configuration

All secrets are read from environment variables; `application.yml` provides development
defaults that should not be used in production. For Docker Compose, copy
[`.env.example`](.env.example) to `.env` and adjust it — Compose picks it up automatically,
and each variable also has a built-in default, so the stack still runs without a `.env` file.

| Variable                     | Purpose                                          | Default                                      |
|------------------------------|--------------------------------------------------|----------------------------------------------|
| `SPRING_DATASOURCE_URL`      | PostgreSQL JDBC URL                              | `jdbc:postgresql://localhost:5432/bank_rest` |
| `SPRING_DATASOURCE_USERNAME` | Database username                                | `bank`                                       |
| `SPRING_DATASOURCE_PASSWORD` | Database password                                | `bank`                                       |
| `APP_JWT_SECRET`             | JWT signing secret (Base64, at least 256 bits)   | dev value                                    |
| `APP_JWT_EXPIRATION_MS`      | Access-token lifetime in milliseconds            | `3600000`                                    |
| `APP_ENCRYPTION_PASSWORD`    | Passphrase for the card-encryption key           | dev value                                    |
| `APP_ENCRYPTION_SALT`        | Hex salt for key derivation                      | dev value                                    |
| `APP_ENCRYPTION_HMAC_KEY`    | HMAC key (Base64)                                | dev value                                    |
| `SERVER_PORT`                | Application port                                 | `8080`                                       |

Generating your own secrets:

```bash
openssl rand -base64 32   # encryption passphrase / HMAC key
openssl rand -hex 8       # encryption salt
openssl rand -base64 48   # JWT secret
```

## Tests

```bash
./mvnw test
```

The suite covers the core logic: transfers including every rejection path (same card,
insufficient funds, blocked, expired, or non-owned card), card issuance, encryption and
masking, and JWT generation and validation. A single end-to-end test drives the real security
chain through MockMvc and runs the actual Liquibase migrations against an in-memory H2 database.

## Project layout

```
src/main/java/com/example/bankcards
├── config         — security, OpenAPI, application properties, MDC request filter
├── controller     — REST controllers (auth, users, cards)
├── dto            — request/response models per area + MapStruct mappers
├── entity         — JPA entities (User, Card); enums live in entity.enums
├── exception      — custom exceptions and the global exception handler
├── repository     — Spring Data JPA interfaces
├── security       — JWT, filter, UserDetails, 401/403 handlers
├── service        — business logic (Auth, User, Card)
├── specification  — dynamic card filters (Criteria API)
└── util           — card-number encryption and masking
src/main/resources/db/migration  — Liquibase changelogs
docs/openapi.yaml                — OpenAPI specification
```
