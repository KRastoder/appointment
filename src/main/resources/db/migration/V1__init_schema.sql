-- =====================================================================
-- Initial schema for the Doctor Appointment Booking backend.
--
-- Flyway is the single source of truth for the database structure.
-- Hibernate runs with ddl-auto=validate and never creates/alters tables.
--
-- Conventions:
--   * surrogate key  : BIGSERIAL (PostgreSQL identity) -> Long in Java
--   * timestamps     : TIMESTAMP WITH TIME ZONE -> OffsetDateTime in Java
--   * money          : NUMERIC(10, 2)            -> BigDecimal in Java
--   * enum-ish text  : VARCHAR + CHECK constraint -> Java enum (STRING)
-- =====================================================================

-- ---------------------------------------------------------------------
-- users (patients / customers)
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id            BIGSERIAL               PRIMARY KEY,
    first_name    VARCHAR(100)            NOT NULL,
    last_name     VARCHAR(100)            NOT NULL,
    email         VARCHAR(320)            NOT NULL,
    password_hash VARCHAR(255)            NOT NULL,
    phone_number  VARCHAR(32),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uq_users_email UNIQUE (email)
);

COMMENT ON TABLE users IS 'Patients who can book appointments. Authentication is NOT implemented yet.';

-- ---------------------------------------------------------------------
-- doctors
-- NOTE: no cached "rating" column on purpose. The average rating is
--       derived from the ratings table (computed later in the service layer).
-- ---------------------------------------------------------------------
CREATE TABLE doctors (
    id              BIGSERIAL                PRIMARY KEY,
    first_name      VARCHAR(100)             NOT NULL,
    last_name       VARCHAR(100)             NOT NULL,
    email           VARCHAR(320)             NOT NULL,
    phone_number    VARCHAR(32),
    specialization  VARCHAR(150)             NOT NULL,
    bio             TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uq_doctors_email UNIQUE (email)
);

CREATE INDEX idx_doctors_specialization ON doctors (specialization);

-- ---------------------------------------------------------------------
-- services (catalog of bookable services)
-- ---------------------------------------------------------------------
CREATE TABLE services (
    id                BIGSERIAL                PRIMARY KEY,
    name              VARCHAR(150)             NOT NULL,
    description       TEXT,
    duration_minutes  INTEGER                  NOT NULL,
    price             NUMERIC(10, 2)           NOT NULL,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uq_services_name UNIQUE (name),
    CONSTRAINT ck_services_duration_minutes CHECK (duration_minutes > 0),
    CONSTRAINT ck_services_price CHECK (price >= 0)
);

-- ---------------------------------------------------------------------
-- doctor_services (join entity: Doctor <-> Service, many-to-many)
-- Composite primary key makes the pair unique.
-- ON DELETE CASCADE: the join rows have no meaning without both parents.
-- Extra columns (price, duration_minutes) can be added here later.
-- ---------------------------------------------------------------------
CREATE TABLE doctor_services (
    doctor_id  BIGINT NOT NULL,
    service_id BIGINT NOT NULL,

    CONSTRAINT pk_doctor_services PRIMARY KEY (doctor_id, service_id),
    CONSTRAINT fk_doctor_services_doctor FOREIGN KEY (doctor_id)
        REFERENCES doctors (id) ON DELETE CASCADE,
    CONSTRAINT fk_doctor_services_service FOREIGN KEY (service_id)
        REFERENCES services (id) ON DELETE CASCADE
);

CREATE INDEX idx_doctor_services_service_id ON doctor_services (service_id);

-- ---------------------------------------------------------------------
-- appointments
-- Scheduling rules (conflicts, availability, transitions) are NOT
-- enforced here: that is application/business logic.
-- ---------------------------------------------------------------------
CREATE TABLE appointments (
    id          BIGSERIAL                PRIMARY KEY,
    user_id     BIGINT                   NOT NULL,
    doctor_id   BIGINT                   NOT NULL,
    service_id  BIGINT                   NOT NULL,
    start_time  TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time    TIMESTAMP WITH TIME ZONE NOT NULL,
    status      VARCHAR(20)              NOT NULL DEFAULT 'SCHEDULED',
    notes       TEXT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_appointments_user FOREIGN KEY (user_id)
        REFERENCES users (id),
    CONSTRAINT fk_appointments_doctor FOREIGN KEY (doctor_id)
        REFERENCES doctors (id),
    CONSTRAINT fk_appointments_service FOREIGN KEY (service_id)
        REFERENCES services (id),
    CONSTRAINT ck_appointments_status CHECK (
        status IN ('SCHEDULED', 'CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW')
    )
);

-- Foreign keys are not indexed automatically in PostgreSQL.
CREATE INDEX idx_appointments_user_id     ON appointments (user_id);
CREATE INDEX idx_appointments_doctor_id   ON appointments (doctor_id);
CREATE INDEX idx_appointments_service_id  ON appointments (service_id);
CREATE INDEX idx_appointments_doctor_time ON appointments (doctor_id, start_time);

-- ---------------------------------------------------------------------
-- ratings (User -> Doctor, referencing the appointment that produced it)
-- UNIQUE (user_id, appointment_id) allows at most one rating per
-- appointment; NULL appointment_id means the rating is not tied to an
-- appointment, and multiple such rows are allowed (NULLs are distinct in
-- a PostgreSQL unique index).
-- "Only a patient who actually had this appointment may rate" is
-- intentionally NOT enforced yet: that is business logic.
-- ---------------------------------------------------------------------
CREATE TABLE ratings (
    id              BIGSERIAL                PRIMARY KEY,
    user_id         BIGINT                   NOT NULL,
    doctor_id       BIGINT                   NOT NULL,
    appointment_id  BIGINT,
    score           SMALLINT                 NOT NULL,
    comment         TEXT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_ratings_user FOREIGN KEY (user_id)
        REFERENCES users (id),
    CONSTRAINT fk_ratings_doctor FOREIGN KEY (doctor_id)
        REFERENCES doctors (id),
    CONSTRAINT fk_ratings_appointment FOREIGN KEY (appointment_id)
        REFERENCES appointments (id),
    CONSTRAINT ck_ratings_score CHECK (score >= 1 AND score <= 5),
    CONSTRAINT uq_ratings_user_appointment UNIQUE (user_id, appointment_id)
);

CREATE INDEX idx_ratings_doctor_id ON ratings (doctor_id);
CREATE INDEX idx_ratings_user_id   ON ratings (user_id);
