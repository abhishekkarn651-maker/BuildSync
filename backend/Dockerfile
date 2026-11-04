# ==============================================================================
# BuildSync Backend — Production Multi-Stage Containerfile
# Java 17 + Spring Boot 3 + Hardened Non-Root Security
# ==============================================================================

# Stage 1: Build & Package
FROM eclipse-temurin:17-jdk-jammy AS builder
WORKDIR /workspace

# Cache Maven wrapper and POM dependencies
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy source code and package application JAR
COPY src src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Minimal Secure Runtime
FROM eclipse-temurin:17-jre-jammy AS runner

# Create dedicated non-root application user and group
RUN groupadd --system --gid 10001 buildsync && \
    useradd --system --uid 10001 --gid 10001 --create-home --shell /bin/false buildsync

# Set up working directory and media upload volume
WORKDIR /app
RUN mkdir -p /var/buildsync/uploads/avatars && \
    chown -R buildsync:buildsync /app /var/buildsync

# Copy packaged JAR from builder stage
COPY --from=builder --chown=buildsync:buildsync /workspace/target/backend-0.0.1-SNAPSHOT.jar app.jar

# Install curl for health checking
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

USER buildsync:buildsync

# Expose Spring Boot default port
EXPOSE 8080

# Health check probing the verified /api/v1/health endpoint
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/health || exit 1

# Container-aware JVM parameters ensuring optimal garbage collection and memory usage
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

