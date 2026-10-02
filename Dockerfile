# =====================================================================
# Stage 1: build the fat jar with Maven + JDK 21
# =====================================================================
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Dependencies first, so this layer is cached as long as the POM does
# not change. Works with the classic builder and with BuildKit.
COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline

# Then the sources.
COPY src ./src
RUN mvn -B -ntp clean package -DskipTests

# =====================================================================
# Stage 2: JRE 21 runtime, no build tooling, no credentials
# =====================================================================
FROM eclipse-temurin:21-jre AS runtime

# Run as a non-root user.
RUN groupadd --system --gid 1001 spring \
 && useradd --system --uid 1001 --gid spring --home /app spring

WORKDIR /app

# Only the built artifact is copied into the runtime image.
COPY --from=build --chown=spring:spring /build/target/doctor-appointment-*.jar app.jar

USER spring:spring

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"

# Connection details are injected at runtime (see docker-compose.yaml).
# Never bake credentials into the image.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
