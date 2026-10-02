package com.keni.doctorappointment.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.keni.doctorappointment.support.PostgresTestSupport;

/**
 * Asserts the database structure that Flyway is expected to create.
 *
 * <p>This is a structural test of the foundation (tables, keys, constraints),
 * not a test of any booking rule.</p>
 */
@SpringBootTest
class FlywaySchemaTest extends PostgresTestSupport {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void flywayCreatesAllTables() {
		assertThat(flywayHistory()).isNotEmpty();

		assertThat(tableNames()).contains("users", "doctors", "services", "doctor_services", "appointments",
				"ratings");
	}

	@Test
	void jwtSupportTablesExist() {
		assertThat(tableNames()).contains("refresh_tokens");
		assertThat(checkConstraints("refresh_tokens")).contains("ck_refresh_tokens_account_type");
		assertThat(uniqueColumns("refresh_tokens", "uq_refresh_tokens_token_hash")).isEqualTo(List.of("token_hash"));
		assertThat(checkConstraints("users")).contains("ck_users_role");
	}

	@Test
	void emailsAreUniqueCaseInsensitively() {
		// the unique constraints were replaced by indexes on LOWER(email)
		assertThat(indexDefinitions()).contains("uq_users_email_lower").contains("uq_doctors_email_lower");
		assertThat(columnType("users", "role")).isEqualTo("character varying");
	}

	@Test
	void doctorsHaveAPasswordHashColumn() {
		assertThat(columnIsNullable("doctors", "password_hash")).isFalse();
	}

	@Test
	void doctorServicesUsesCompositePrimaryKey() {
		assertThat(primaryKeyColumns("doctor_services")).containsExactly("doctor_id", "service_id");
	}

	@Test
	void serviceNameIsUnique() {
		assertThat(uniqueColumns("services", "uq_services_name")).isEqualTo(List.of("name"));
	}

	@Test
	void ratingConstraintsExist() {
		assertThat(uniqueColumns("ratings", "uq_ratings_user_appointment"))
				.isEqualTo(List.of("user_id", "appointment_id"));
		assertThat(checkConstraints("ratings")).contains("ck_ratings_score");
		assertThat(checkConstraints("appointments")).contains("ck_appointments_status");
	}

	@Test
	void appointmentHasTimezoneAwareTimestamps() {
		assertThat(columnDataType("appointments", "start_time")).isEqualTo("timestamp with time zone");
		assertThat(columnDataType("appointments", "end_time")).isEqualTo("timestamp with time zone");
	}

	@Test
	void foreignKeysArePresent() {
		assertThat(foreignKeys("appointments"))
				.containsExactlyInAnyOrder("user_id", "doctor_id", "service_id");
		assertThat(foreignKeys("ratings"))
				.containsExactlyInAnyOrder("user_id", "doctor_id", "appointment_id");
		assertThat(foreignKeys("doctor_services")).containsExactlyInAnyOrder("doctor_id", "service_id");
	}

	@Test
	void scoreCheckConstraintRejectsOutOfRangeValues() {
		assertThat(columnIsNullable("ratings", "score")).isFalse();

		jdbcTemplate.update("INSERT INTO doctors (first_name, last_name, email, specialization) "
				+ "VALUES ('Ada', 'Lovelace', 'ada@example.org', 'Cardiology')");
		jdbcTemplate.update("INSERT INTO users (first_name, last_name, email, password_hash) "
				+ "VALUES ('Alan', 'Turing', 'alan@example.org', 'hash')");
		Long doctorId = jdbcTemplate.queryForObject(
				"SELECT id FROM doctors WHERE email = 'ada@example.org'", Long.class);
		Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE email = 'alan@example.org'", Long.class);

		assertThatThrownByInsert(userId, doctorId, (short) 0);
		assertThatThrownByInsert(userId, doctorId, (short) 6);
	}

	private void assertThatThrownByInsert(Long userId, Long doctorId, short score) {
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbcTemplate.update(
				"INSERT INTO ratings (user_id, doctor_id, score) VALUES (?, ?, ?)", userId, doctorId, score))
			.isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
	}

	// --- helpers -------------------------------------------------------

	private List<String> flywayHistory() {
		return jdbcTemplate.queryForList("SELECT version FROM flyway_schema_history WHERE success = true",
				String.class);
	}

	private List<String> tableNames() {
		return jdbcTemplate.queryForList("""
				SELECT table_name
				FROM information_schema.tables
				WHERE table_schema = 'public'
				  AND table_type = 'BASE TABLE'
				""", String.class);
	}

	private List<String> primaryKeyColumns(String table) {
		return jdbcTemplate.queryForList("""
				SELECT a.attname
				FROM pg_index i
				JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY(i.indkey)
				WHERE i.indrelid = ?::regclass AND i.indisprimary
				ORDER BY a.attnum
				""", String.class, table);
	}

	private List<String> uniqueColumns(String table, String constraintName) {
		return jdbcTemplate.queryForList("""
				SELECT a.attname
				FROM pg_constraint c
				JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
				WHERE c.conrelid = ?::regclass AND c.conname = ?
				ORDER BY a.attnum
				""", String.class, table, constraintName);
	}

	private List<String> checkConstraints(String table) {
		return jdbcTemplate.queryForList("""
				SELECT c.conname
				FROM pg_constraint c
				WHERE c.conrelid = ?::regclass AND c.contype = 'c'
				""", String.class, table);
	}

	private List<String> foreignKeys(String table) {
		return jdbcTemplate.queryForList("""
				SELECT a.attname
				FROM pg_constraint c
				JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
				WHERE c.conrelid = ?::regclass AND c.contype = 'f'
				""", String.class, table);
	}

	private String columnDataType(String table, String column) {
		return jdbcTemplate.queryForObject("""
				SELECT data_type
				FROM information_schema.columns
				WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
				""", String.class, table, column);
	}

	private List<String> indexNames(String table) {
		return jdbcTemplate.queryForList("SELECT indexname FROM pg_indexes WHERE tablename = ?", String.class, table);
	}

	private List<String> indexDefinitions() {
		return jdbcTemplate.queryForList("SELECT indexname FROM pg_indexes WHERE schemaname = 'public'",
				String.class);
	}

	private boolean columnIsNullable(String table, String column) {
		return "YES".equals(jdbcTemplate.queryForObject("""
				SELECT is_nullable
				FROM information_schema.columns
				WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
				""", String.class, table, column));
	}

}
