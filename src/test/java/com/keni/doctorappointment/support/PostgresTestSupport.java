package com.keni.doctorappointment.support;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for tests that need a real PostgreSQL schema.
 *
 * <p>A throw-away PostgreSQL instance is started by Testcontainers, Flyway
 * applies the migrations and Hibernate validates the mapping against the
 * resulting schema (H2/embedded databases are deliberately not used: they
 * would not catch PostgreSQL specific DDL problems).</p>
 *
 * <p>{@code disabledWithoutDocker = true} makes the test suite pass (skipped)
 * on machines without a Docker daemon, so {@code ./mvnw test} never fails for
 * environmental reasons. With Docker available the tests run for real.</p>
 */
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgresTestSupport {

	@Container
	protected static final PostgreSQLContainer<?> POSTGRES = createPostgresContainer();

	private static PostgreSQLContainer<?> createPostgresContainer() {
		PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:17-alpine");
		container.withDatabaseName("doctor_appointments");
		container.withUsername("doctor");
		container.withPassword("doctor");
		return container;
	}

	@DynamicPropertySource
	static void postgresProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
	}

}
