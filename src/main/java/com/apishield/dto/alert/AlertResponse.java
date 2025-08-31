package com.apishield.dto.alert;

import com.apishield.model.Alert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponse {
    private Long id;
    private String apiServiceName;
    private Alert.AlertType alertType;
    private String message;
    private String formattedMessage;
    private Alert.Severity severity;
    private Double thresholdValue;
    private Double actualValue;
    private Boolean isResolved;
    private LocalDateTime resolvedAt;
    private Boolean notificationSent;
    private LocalDateTime createdAt;
    private long minutesSinceCreated;

    // Additional context
    private String alertTypeDescription;
    private String severityColor; // For UI styling
    private String actionRequired;
}
