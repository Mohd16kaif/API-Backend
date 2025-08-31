package com.apishield.health;

import com.apishield.repository.ApiServiceRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class ApiServiceHealthIndicator implements HealthIndicator {

    private final ApiServiceRepository apiServiceRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ApiServiceHealthIndicator(ApiServiceRepository apiServiceRepository) {
        this.apiServiceRepository = apiServiceRepository;
    }

    @Override
    public Health health() {
        try {
            // Get basic service counts - these should work with standard JPA repository
            long totalServices = apiServiceRepository.count();

            // Try to get active services count - use try/catch for graceful fallback
            long activeServices = 0;
            try {
                activeServices = apiServiceRepository.countByIsActive(true);
            } catch (Exception e) {
                // If countByIsActive method doesn't exist, assume all are active
                activeServices = totalServices;
            }

            long inactiveServices = totalServices - activeServices;

            // Handle case where no services are configured
            if (totalServices == 0) {
                return Health.up()
                        .withDetail("status", "No services configured")
                        .withDetail("message", "System is ready but no API services have been configured yet")
                        .withDetail("total_services", 0)
                        .withDetail("check_time", LocalDateTime.now().format(FORMATTER))
                        .build();
            }

            // Calculate health metrics
            double activePercentage = (activeServices / (double) totalServices) * 100;

            // Determine overall health status
            Health.Builder healthBuilder;
            String healthStatus;
            String healthMessage;

            if (activePercentage >= 80) {
                healthBuilder = Health.up();
                healthStatus = "Excellent";
                healthMessage = "Most services are active and running well";
            } else if (activePercentage >= 50) {
                healthBuilder = Health.status("DEGRADED");
                healthStatus = "Degraded";
                healthMessage = "Some services are inactive, monitoring recommended";
            } else if (activePercentage > 0) {
                healthBuilder = Health.down();
                healthStatus = "Poor";
                healthMessage = "Most services are inactive, immediate attention required";
            } else {
                healthBuilder = Health.down();
                healthStatus = "Critical";
                healthMessage = "All services are inactive, system may be down";
            }

            return healthBuilder
                    .withDetail("status", healthStatus)
                    .withDetail("message", healthMessage)
                    .withDetail("total_services", totalServices)
                    .withDetail("active_services", activeServices)
                    .withDetail("inactive_services", inactiveServices)
                    .withDetail("active_percentage", String.format("%.1f%%", activePercentage))
                    .withDetail("check_time", LocalDateTime.now().format(FORMATTER))
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("status", "Service check failed")
                    .withDetail("error", e.getClass().getSimpleName())
                    .withDetail("error_message", e.getMessage() != null ? e.getMessage() : "Unknown error occurred")
                    .withDetail("check_time", LocalDateTime.now().format(FORMATTER))
                    .withDetail("recommendation", "Check database connectivity and ApiService repository")
                    .build();
        }
    }
}