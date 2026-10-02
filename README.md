# Doctor Appointment Booking — Backend

A modular monolith in Java 21 / Spring Boot for booking doctor appointments:
patients book a service with a doctor, doctors approve their appointments,
staff manages the catalogue. PostgreSQL + Flyway + JPA/Hibernate, simple
hand-rolled HS256 JWT auth (no OAuth2 libraries), Docker Compose for one
command startup.

> Foundation + auth. Booking/availability/cancellation rules are deliberately
> left as TODOs (see `appointments/AppointmentService`).

## Quick start

```bash
cp .env.example .env       # adjust if you like; defaults work locally
docker compose up --build  # app on :8080, postgres on :5432
```

Flyway migrates the schema on startup and Hibernate validates it
(`ddl-auto=validate`; Hibernate never creates tables).

The first start creates a bootstrap ADMIN account so you can sign in:

| account                          | password     | role  |
|----------------------------------|--------------|-------|
| `admin@doctor-appointment.local` | `Admin12345` | ADMIN |

Configure/disable it via `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD`.
Change the password after the first login.

### Local development without Docker for the app

```bash
docker compose up -d db           # only postgres
DB_HOST=localhost DB_PASSWORD=doctor ./mvnw spring-boot:run
```

### Tests

```bash
./mvnw test
```

Integration tests run against a real PostgreSQL started by Testcontainers
and are skipped automatically when no Docker daemon is available.

## Architecture

```
app (Spring Boot, Java 21)          db (PostgreSQL 17)
├─ users        (patients + staff)  ├─ users
├─ doctors      (doctor accounts)   ├─ doctors
├─ services     (catalogue)         ├─ services
├─ doctorservices (join entity)     ├─ doctor_services  PK (doctor_id, service_id)
├─ appointments (booking core)      ├─ appointments
├─ ratings      (doctor feedback)   ├─ ratings
├─ auth         (JWT login/refresh) ├─ refresh_tokens   (hashed tokens)
├─ security     (JWT, principal)    └─ flyway_schema_history
└─ config       (wiring, errors)
```

Package-by-feature: each module keeps its entity, repository, service,
controller and DTOs together. Migrations are the single source of truth
(`src/main/resources/db/migration/V1…V3`).

## Authentication

Three account types: **PATIENT** (`ROLE_USER`), **DOCTOR** (`ROLE_DOCTOR`),
**ADMIN** (`ROLE_ADMIN`, a `users` row with `role=ADMIN`).

* Access token: short-lived JWT in `Authorization: Bearer …`
* Refresh token: long-lived, rotated on every refresh, stored **hashed** so
  logout really revokes it
* E-mails are stored lower-case; a unique index on `LOWER(email)` and a
  cross-table check in the service layer keep them unique case-insensitively

```bash
# register (patient) and log in
curl -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"firstName":"Alan","lastName":"Turing","email":"alan@example.org","password":"Enigma123"}'
curl -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"alan@example.org","password":"Enigma123"}'
# -> { accessToken, refreshToken, tokenType, expiresIn, accountId, email, accountType }

curl localhost:8080/api/appointments/me -H "Authorization: Bearer $ACCESS_TOKEN"
```

| method | path                    | public? | description                            |
|--------|-------------------------|---------|----------------------------------------|
| POST   | `/api/auth/register`    | yes     | patient self registration              |
| POST   | `/api/auth/login`       | yes     | e-mail + password → token pair         |
| POST   | `/api/auth/refresh`     | yes     | new pair, old refresh token is revoked |
| POST   | `/api/auth/logout`      | yes     | revokes the refresh token              |
| GET    | `/api/auth/me`          | token   | who am I                               |

## Endpoints

Appointments (the doctor only ever sees his own):

| method | path                            | who           | description                       |
|--------|---------------------------------|---------------|-----------------------------------|
| GET    | `/api/appointments/me`          | any account   | own appointments                  |
| GET    | `/api/appointments/me/pending`  | doctor        | own `SCHEDULED` appointments      |
| GET    | `/api/appointments/me/approved` | doctor        | own `CONFIRMED` appointments      |
| GET    | `/api/appointments/{id}`        | parties only  | one appointment                   |
| POST   | `/api/appointments/{id}/approve`| owning doctor | `SCHEDULED` → `CONFIRMED`         |

Users (patients + staff):

| method | path                    | who              |
|--------|-------------------------|------------------|
| GET    | `/api/users/me`         | patient/admin    |
| PUT    | `/api/users/me`         | patient/admin    |
| PUT    | `/api/users/me/password`| patient/admin    |
| DELETE | `/api/users/me`         | patient/admin    |
| POST   | `/api/users`            | ADMIN            |
| GET    | `/api/users`            | ADMIN            |
| GET/PUT/DELETE | `/api/users/{id}` | self or ADMIN    |

Doctors and services:

| method | path                                    | who     |
|--------|-----------------------------------------|---------|
| GET    | `/api/doctors`, `/api/doctors/{id}`     | public  |
| GET    | `/api/doctors/me`                       | doctor  |
| PUT    | `/api/doctors/me/password`              | doctor  |
| POST   | `/api/doctors`                          | ADMIN   |
| PUT    | `/api/doctors/{id}`                     | self/ADMIN |
| DELETE | `/api/doctors/{id}`                     | ADMIN   |
| POST   | `/api/doctors/{id}/services/{sid}`      | ADMIN   |
| DELETE | `/api/doctors/{id}/services/{sid}`      | ADMIN   |
| GET    | `/api/services`, `/api/services/{id}`   | public  |
| POST/PUT/DELETE | `/api/services[/{id}]`           | ADMIN   |
| GET    | `/api/ratings`, `/api/ratings/{id}`     | public  |

Errors are RFC 7807 problem details: `400` validation, `401` bad/missing
token or credentials, `403` wrong role/owner, `404` missing resource,
`409` duplicate e-mail / illegal status change / delete with dependencies.

## Configuration

All environment variables live in `.env.example` (copy to `.env`):

* `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
* `JWT_SECRET` (≥ 32 bytes), `JWT_ACCESS_TOKEN_TTL`, `JWT_REFRESH_TOKEN_TTL`
* `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`
* `APP_PORT`, `SERVER_PORT`

## TODO (deliberately not implemented)

* Booking: availability checks, overlap detection, end time from service
  duration, optimistic locking (`version` column)
* Cancellation / rescheduling rules, status transitions beyond approve
* Doctor availability (working hours, time off)
* Ratings: who may rate whom, average rating
* Notifications, payments
