package com.apishield.scheduler;

import com.apishield.model.User;
import com.apishield.repository.UserRepository;
import com.apishield.service.AlertService;
import com.apishield.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlertScheduler {

    private final AlertService alertService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    /**
     * Daily alert check - runs every day at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void dailyAlertCheck() {
        log.info("Starting daily alert check...");

        try {
            List<User> users = userRepository.findAll();
            int alertsGenerated = 0;

            for (User user : users) {
                try {
                    alertService.checkAndGenerateAlerts(user);
                    alertsGenerated++;
                } catch (Exception e) {
                    log.error("Error checking alerts for user: {}", user.getEmail(), e);
                }
            }

            log.info("Completed daily alert check for {} users", alertsGenerated);
        } catch (Exception e) {
            log.error("Error during daily alert check", e);
        }
    }

    /**
     * Process notification queue - runs every 15 minutes
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    public void processNotificationQueue() {
        log.debug("Processing notification queue...");

        try {
            notificationService.sendBatchNotifications();
        } catch (Exception e) {
            log.error("Error processing notification queue", e);
        }
    }

    /**
     * Hourly alert check for critical services - runs every hour
     */
    @Scheduled(cron = "0 0 * * * *")
    public void hourlyAlertCheck() {
        log.debug("Starting hourly critical alert check...");

        try {
            List<User> users = userRepository.findAll();

            for (User user : users) {
                try {
                    // Only check for immediate/critical alerts more frequently
                    alertService.checkAndGenerateAlerts(user);
                } catch (Exception e) {
                    log.error("Error in hourly alert check for user: {}", user.getEmail(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error during hourly alert check", e);
        }
    }

    /**
     * Weekly alert summary - runs every Sunday at 9 AM
     */
    @Scheduled(cron = "0 0 9 * * SUN")
    public void weeklyAlertSummary() {
        log.info("Generating weekly alert summaries...");

        try {
            List<User> users = userRepository.findAll();

            for (User user : users) {
                try {
                    // Implementation for weekly summary would go here
                    // For now, just log that we would send it
                    log.debug("Would send weekly summary to user: {}", user.getEmail());
                } catch (Exception e) {
                    log.error("Error generating weekly summary for user: {}", user.getEmail(), e);
                }
            }
        } catch (Exception e) {
            log.error("Error during weekly alert summary generation", e);
        }
    }
}
