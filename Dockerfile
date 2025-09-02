# Use OpenJDK 21 (matching your project's Java version)
FROM eclipse-temurin:21-jre

# Set working directory
WORKDIR /app

# Create non-root user
RUN useradd -r -s /bin/false apiuser

# Create logs directory
RUN mkdir -p logs && chown apiuser:apiuser logs

# Copy the JAR file (corrected artifact name)
COPY target/apishield-*.jar app.jar

# Change ownership to non-root user
RUN chown apiuser:apiuser app.jar

# Install curl for health check (run as root BEFORE switching user)
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Switch to non-root user AFTER installing everything
USER apiuser

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app/app.jar"]
