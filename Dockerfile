# Multi-stage build
# Stage 1: Build the application
FROM eclipse-temurin:21-jdk AS builder

# Set working directory
WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Copy source code
COPY src src

# Make mvnw executable
RUN chmod +x mvnw

# Build the application (skip tests for faster builds)
RUN ./mvnw clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre

# Set working directory
WORKDIR /app

# Install curl for health check BEFORE creating user
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN useradd -r -s /bin/false apiuser

# Create logs directory
RUN mkdir -p logs && chown apiuser:apiuser logs

# Copy the JAR file from builder stage
COPY --from=builder /app/target/apishield-*.jar app.jar

# Change ownership to non-root user
RUN chown apiuser:apiuser app.jar

# Switch to non-root user
USER apiuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]