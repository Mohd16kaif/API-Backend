package com.apishield.service;

import com.apishield.model.Alert;
import com.apishield.model.User;
import com.apishield.repository.AlertRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final AlertRepository alertRepository;

    /**
     * Send notification for a single alert
     */
    @Transactional
    public void sendAlertNotification(Alert alert) {
        try {
            User user = alert.getUser();
            String subject = buildEmailSubject(alert);
            String body = buildEmailBody(alert);

            sendEmail(user.getEmail(), subject, body);

            alert.markNotificationSent();
            alertRepository.save(alert);

            log.info("Sent alert notification to user: {} for alert: {}",
                    user.getEmail(), alert.getAlertType());
        } catch (Exception e) {
            log.error("Failed to send alert notification for alert ID: {}", alert.getId(), e);
        }
    }

    /**
     * Send notifications for multiple alerts (batch processing)
     */
    @Transactional
    public void sendBatchNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30); // Don't send very old notifications
        List<Alert> unsentAlerts = alertRepository.findUnsentNotifications(cutoff);

        log.info("Processing {} unsent alert notifications", unsentAlerts.size());

        for (Alert alert : unsentAlerts) {
            sendAlertNotification(alert);
        }
    }

    /**
     * Send daily alert summary to user
     */
    public void sendDailySummary(User user, List<Alert> dailyAlerts) {
        if (dailyAlerts.isEmpty()) {
            return; // No alerts to summarize
        }

        try {
            String subject = String.format("API Spend Shield - Daily Alert Summary (%d alerts)",
                    dailyAlerts.size());
            String body = buildDailySummaryBody(user, dailyAlerts);

            sendEmail(user.getEmail(), subject, body);

            log.info("Sent daily alert summary to user: {} with {} alerts",
                    user.getEmail(), dailyAlerts.size());
        } catch (Exception e) {
            log.error("Failed to send daily summary to user: {}", user.getEmail(), e);
        }
    }

    /**
     * Send test notification
     */
    public void sendTestNotification(String email) {
        try {
            String subject = "API Spend Shield - Test Notification";
            String body = "This is a test notification from API Spend Shield. " +
                    "Your alert notifications are configured correctly!";

            sendEmail(email, subject, body);
            log.info("Sent test notification to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send test notification to: {}", email, e);
            throw new RuntimeException("Failed to send test notification", e);
        }
    }

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@apispendshield.com");
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);

            // In development, just log the email instead of sending
            if (isDevelopmentMode()) {
                log.info("EMAIL NOTIFICATION (DEV MODE):");
                log.info("To: {}", to);
                log.info("Subject: {}", subject);
                log.info("Body: {}", body);
                log.info("--- END EMAIL ---");
            } else {
                mailSender.send(message);
            }
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw e;
        }
    }

    private String buildEmailSubject(Alert alert) {
        String prefix = switch (alert.getSeverity()) {
            case CRITICAL -> "🚨 CRITICAL";
            case HIGH -> "⚠️ HIGH";
            case MEDIUM -> "⚡ MEDIUM";
            case LOW -> "ℹ️ LOW";
        };

        String apiName = alert.getApiService() != null ?
                alert.getApiService().getName() : "System";

        return String.format("%s Alert - %s | API Spend Shield", prefix, apiName);
    }

    private String buildEmailBody(Alert alert) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

        StringBuilder body = new StringBuilder();
        body.append("Hello,\n\n");
        body.append("An alert has been triggered in your API Spend Shield dashboard.\n\n");
        body.append("ALERT DETAILS:\n");
        body.append("=============\n");
        body.append("API Service: ").append(alert.getApiService() != null ?
                alert.getApiService().getName() : "System").append("\n");
        body.append("Alert Type: ").append(alert.getAlertType().name().replace("_", " ")).append("\n");
        body.append("Severity: ").append(alert.getSeverity().name()).append("\n");
        body.append("Message: ").append(alert.getMessage()).append("\n");
        body.append("Time: ").append(alert.getCreatedAt().format(formatter)).append("\n");

        if (alert.getThresholdValue() != null && alert.getActualValue() != null) {
            body.append("Threshold: ").append(alert.getThresholdValue()).append("\n");
            body.append("Actual Value: ").append(alert.getActualValue()).append("\n");
        }

        body.append("\nRECOMMENDED ACTION:\n");
        body.append("==================\n");
        body.append(getActionRecommendation(alert)).append("\n");

        body.append("\nYou can view and manage this alert in your dashboard at: ");
        body.append("https://app.apispendshield.com/alerts\n\n");
        body.append("Best regards,\n");
        body.append("API Spend Shield Team\n\n");
        body.append("---\n");
        body.append("To modify your alert settings, visit: https://app.apispendshield.com/settings/alerts");

        return body.toString();
    }

    private String buildDailySummaryBody(User user, List<Alert> dailyAlerts) {
        StringBuilder body = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        body.append("Hello ").append(user.getName()).append(",\n\n");
        body.append("Here's your daily alert summary for ").append(LocalDate.now().format(formatter)).append(":\n\n");

        // Group alerts by severity
        long critical = dailyAlerts.stream().filter(a -> a.getSeverity() == Alert.Severity.CRITICAL).count();
        long high = dailyAlerts.stream().filter(a -> a.getSeverity() == Alert.Severity.HIGH).count();
        long medium = dailyAlerts.stream().filter(a -> a.getSeverity() == Alert.Severity.MEDIUM).count();
        long low = dailyAlerts.stream().filter(a -> a.getSeverity() == Alert.Severity.LOW).count();

        body.append("ALERT SUMMARY:\n");
        body.append("==============\n");
        if (critical > 0) body.append("🚨 Critical: ").append(critical).append("\n");
        if (high > 0) body.append("⚠️ High: ").append(high).append("\n");
        if (medium > 0) body.append("⚡ Medium: ").append(medium).append("\n");
        if (low > 0) body.append("ℹ️ Low: ").append(low).append("\n");
        body.append("Total: ").append(dailyAlerts.size()).append(" alerts\n\n");

        // Show critical and high alerts in detail
        List<Alert> importantAlerts = dailyAlerts.stream()
                .filter(a -> a.getSeverity() == Alert.Severity.CRITICAL || a.getSeverity() == Alert.Severity.HIGH)
                .limit(5)
                .toList();

        if (!importantAlerts.isEmpty()) {
            body.append("IMPORTANT ALERTS:\n");
            body.append("=================\n");
            for (Alert alert : importantAlerts) {
                body.append("• ").append(alert.getFormattedMessage()).append("\n");
            }
            body.append("\n");
        }

        body.append("View all alerts in your dashboard: https://app.apispendshield.com/alerts\n\n");
        body.append("Best regards,\n");
        body.append("API Spend Shield Team");

        return body.toString();
    }

    private String getActionRecommendation(Alert alert) {
        return switch (alert.getAlertType()) {
            case BUDGET_WARNING -> "Monitor your API usage closely and consider optimizing calls to stay within budget.";
            case BUDGET_CRITICAL -> "Immediate action required: optimize API usage or increase your budget to avoid service interruption.";
            case USAGE_SPIKE -> "Investigate the cause of this usage spike to ensure it's legitimate and expected.";
            case HIGH_ERROR_RATE -> "Check your API service status and implement better error handling in your application.";
            case SERVICE_DOWN -> "Contact your API service provider and implement fallback mechanisms.";
            case COST_ANOMALY -> "Review your recent usage patterns and validate any unexpected charges.";
        };
    }

    private boolean isDevelopmentMode() {
        // Check if we're in development mode (no real email sending)
        return !mailSender.getClass().getName().contains("JavaMailSenderImpl") ||
                System.getProperty("spring.profiles.active", "").contains("test");
    }
}
