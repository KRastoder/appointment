-- =====================================================================
-- JWT authentication support.
--
--  1. refresh_tokens : server side store so that logout really invalidates
--                      a refresh token (only a SHA-256 hash is stored).
--  2. users.role     : PATIENT or ADMIN (staff account that manages
--                      services and doctors).
--  3. case insensitive, unique e-mails for users and doctors.
--                      E-mails are stored lower case; the unique index on
--                      LOWER(email) makes "Ada@x.org" and "ada@x.org"
--                      collide.
-- =====================================================================

-- ---------------------------------------------------------------------
-- Refresh tokens (rotated on every refresh, revoked on logout)
-- ---------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id            BIGSERIAL                PRIMARY KEY,
    account_type  VARCHAR(20)              NOT NULL,
    account_id    BIGINT                   NOT NULL,
    -- SHA-256 hex digest of the token; the token itself is never stored.
    token_hash    VARCHAR(64)              NOT NULL,
    issued_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at    TIMESTAMP WITH TIME ZONE,

    CONSTRAINT ck_refresh_tokens_account_type CHECK (account_type IN ('PATIENT', 'DOCTOR', 'ADMIN')),
    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_tokens_account ON refresh_tokens (account_type, account_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens (expires_at);

COMMENT ON TABLE refresh_tokens IS 'Hashed refresh tokens; revoked_at set on logout/rotation.';

-- ---------------------------------------------------------------------
-- ADMIN role for staff accounts
-- ---------------------------------------------------------------------
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'PATIENT';

ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('PATIENT', 'ADMIN'));

COMMENT ON COLUMN users.role IS 'PATIENT = booking customer, ADMIN = manages services and doctors.';

-- ---------------------------------------------------------------------
-- Case insensitive unique e-mails
-- ---------------------------------------------------------------------
ALTER TABLE users DROP CONSTRAINT uq_users_email;
CREATE UNIQUE INDEX uq_users_email_lower ON users (LOWER(email));

ALTER TABLE doctors DROP CONSTRAINT uq_doctors_email;
CREATE UNIQUE INDEX uq_doctors_email_lower ON doctors (LOWER(email));
