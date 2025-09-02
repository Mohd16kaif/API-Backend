# ---- Stage 1: Build JAR using Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy Maven files and source code
COPY pom.xml .
COPY src ./src

# Build JAR (skip tests for faster build)
RUN mvn clean package -DskipTests

# ---- Stage 2: Run the application ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Create non-root user
RUN useradd -r -s /bin/false apiuser

# Create logs directory
RUN mkdir -p logs && chown apiuser:apiuser logs

# Copy built JAR from build stage
COPY --from=build /app/target/apishield-*.jar app.jar

# Change ownership to non-root user
RUN chown apiuser:apiuser app.jar

# Switch to non-root user
USER apiuser

# Expose port
EXPOSE 8080

# Install curl for health check
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
