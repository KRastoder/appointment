package com.keni.doctorappointment.support;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for tests that need a real PostgreSQL schema.
 *
 * <p>Singleton container: one throw-away PostgreSQL is started manually for
 * the whole test JVM and shared by every test class. This matters because
 * Spring caches the application context (and its datasource) between test
 * classes - if every class started and stopped its own container, a cached
 * datasource would point at a dead port ("connection refused"). Ryuk cleans
 * the container up when the JVM exits.</p>
 *
 * <p>{@code disabledWithoutDocker = true} skips the whole class when no
 * Docker daemon is reachable, so {@code ./mvnw test} also passes on machines
 * without Docker.</p>
 */
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
public abstract class PostgresTestSupport {

	protected static final PostgreSQLContainer POSTGRES = startPostgres();

	private static PostgreSQLContainer startPostgres() {
		PostgreSQLContainer container = new PostgreSQLContainer("postgres:17-alpine");
		container.withDatabaseName("doctor_appointments");
		container.withUsername("doctor");
		container.withPassword("doctor");
		container.start();
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
