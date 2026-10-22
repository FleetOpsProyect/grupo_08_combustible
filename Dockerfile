# ── Stage 1: build the jar with the Maven Wrapper (tests already ran in the CI pipeline) ──
FROM eclipse-temurin:21.0.12_8-jdk-alpine-3.24 AS builder
WORKDIR /workspace

# Dependencies first, so this layer is cached while pom.xml does not change
COPY .mvn/ .mvn/
COPY mvnw pom.xml checkstyle.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q package -DskipTests -Dcheckstyle.skip -Djacoco.skip

# ── Stage 2: runtime with the JRE only, as a non-root user ──
FROM eclipse-temurin:21.0.12_8-jre-alpine-3.24
RUN addgroup -S fuel && adduser -S fuel -G fuel
WORKDIR /app
COPY --from=builder --chown=fuel:fuel /workspace/target/fleetops-fuel-service.jar app.jar
USER fuel

# The port comes from SERVER_PORT; memory adapts to the container limit (t3.micro on EC2)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
