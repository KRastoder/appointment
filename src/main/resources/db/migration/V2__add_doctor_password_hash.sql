-- =====================================================================
-- Doctors must be able to log in, so they need a credential column.
-- Added as a separate migration to keep V1 an immutable snapshot.
--
-- NOTE: only the hash is stored; BCrypt is used by default
-- (see SecurityConfig#passwordEncoder).
-- =====================================================================
ALTER TABLE doctors
    ADD COLUMN password_hash VARCHAR(255) NOT NULL;

COMMENT ON COLUMN doctors.password_hash IS 'BCrypt hash of the doctor password. Never store plain text.';
