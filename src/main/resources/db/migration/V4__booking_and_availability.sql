-- =====================================================================
-- Booking rules: doctor availability, optimistic locking and real
-- double-booking protection.
--
--  1. appointments.version       : optimistic locking (@Version) for
--                                  concurrent status changes.
--  2. doctor_availability        : weekly working hours (1..7 = Mon..Sun).
--  3. doctor_time_off            : holidays / vacations / blocked periods.
--  4. exclusion constraint       : makes overlapping appointments for the
--                                  same doctor impossible AT THE DATABASE,
--                                  no matter what the application does.
-- =====================================================================

ALTER TABLE appointments ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN appointments.version IS 'Optimistic locking token; Hibernate increments on every change.';

-- ---------------------------------------------------------------------
-- Weekly working hours. A doctor may have several windows per day.
-- ---------------------------------------------------------------------
CREATE TABLE doctor_availability (
    id           BIGSERIAL  PRIMARY KEY,
    doctor_id    BIGINT     NOT NULL,
    day_of_week  INTEGER    NOT NULL,
    start_time   TIME       NOT NULL,
    end_time     TIME       NOT NULL,

    CONSTRAINT fk_doctor_availability_doctor FOREIGN KEY (doctor_id)
        REFERENCES doctors (id) ON DELETE CASCADE,
    CONSTRAINT ck_doctor_availability_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_doctor_availability_times CHECK (end_time > start_time),
    CONSTRAINT uq_doctor_availability UNIQUE (doctor_id, day_of_week, start_time)
);

CREATE INDEX idx_doctor_availability_doctor ON doctor_availability (doctor_id);

COMMENT ON TABLE doctor_availability IS 'Weekly working hours; times are in the doctor local time.';

-- ---------------------------------------------------------------------
-- Blocked periods (vacation, sick leave, ...).
-- ---------------------------------------------------------------------
CREATE TABLE doctor_time_off (
    id         BIGSERIAL                PRIMARY KEY,
    doctor_id  BIGINT                   NOT NULL,
    start_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    reason     TEXT,

    CONSTRAINT fk_doctor_time_off_doctor FOREIGN KEY (doctor_id)
        REFERENCES doctors (id) ON DELETE CASCADE,
    CONSTRAINT ck_doctor_time_off_times CHECK (end_at > start_at)
);

CREATE INDEX idx_doctor_time_off_doctor ON doctor_time_off (doctor_id);

-- ---------------------------------------------------------------------
-- Hard double-booking protection.
-- An appointment may not overlap another SCHEDULED/CONFIRMED appointment
-- of the same doctor. (CANCELLED/COMPLETED/NO_SHOW free the slot again.)
-- btree_gist ships with every official PostgreSQL image.
-- ---------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE appointments ADD CONSTRAINT ex_appointments_no_doctor_overlap
    EXCLUDE USING gist (
        doctor_id WITH =,
        tstzrange(start_time, end_time) WITH &&
    ) WHERE (status IN ('SCHEDULED', 'CONFIRMED'));
