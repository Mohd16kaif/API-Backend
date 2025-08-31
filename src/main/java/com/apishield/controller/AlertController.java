package com.apishield.controller;

import com.apishield.dto.alert.*;
import com.apishield.model.User;
import com.apishield.service.AlertService;
import com.apishield.service.NotificationService;
import com.apishield.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Alerts", description = "Alert configuration and management APIs")
public class AlertController {

    private final AlertService alertService;
    private final NotificationService notificationService;
    private final UserService userService;

    @PostMapping("/thresholds")
    @Operation(summary = "Create or update alert thresholds for an API service")
    public ResponseEntity<AlertThresholdResponse> createOrUpdateThreshold(
            Authentication authentication,
            @Valid @RequestBody AlertThresholdRequest request) {
        User user = userService.getCurrentUserEntity(authentication);
        AlertThresholdResponse response = alertService.createOrUpdateThreshold(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all alert thresholds for the current user")
    public ResponseEntity<List<AlertThresholdResponse>> getAllThresholds(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);
        List<AlertThresholdResponse> thresholds = alertService.getAllThresholds(user);
        return ResponseEntity.ok(thresholds);
    }

    @DeleteMapping("/thresholds/{id}")
    @Operation(summary = "Delete an alert threshold")
    public ResponseEntity<Void> deleteThreshold(
            Authentication authentication,
            @Parameter(description = "Threshold ID") @PathVariable Long id) {
        User user = userService.getCurrentUserEntity(authentication);
        alertService.deleteThreshold(user, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/activity")
    @Operation(summary = "Get alert activity and statistics")
    public ResponseEntity<AlertActivityResponse> getAlertActivity(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);
        AlertActivityResponse activity = alertService.getAlertActivity(user);
        return ResponseEntity.ok(activity);
    }

    @PutMapping("/{id}/resolve")
    @Operation(summary = "Resolve an alert")
    public ResponseEntity<AlertResponse> resolveAlert(
            Authentication authentication,
            @Parameter(description = "Alert ID") @PathVariable Long id) {
        User user = userService.getCurrentUserEntity(authentication);
        AlertResponse response = alertService.resolveAlert(user, id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/test-notification")
    @Operation(summary = "Send a test notification to verify email configuration")
    public ResponseEntity<Map<String, String>> sendTestNotification(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);

        try {
            notificationService.sendTestNotification(user.getEmail());
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Test notification sent to " + user.getEmail()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "Failed to send test notification: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/check-now")
    @Operation(summary = "Manually trigger alert check for current user")
    public ResponseEntity<Map<String, String>> checkAlertsNow(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);

        try {
            alertService.checkAndGenerateAlerts(user);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Alert check completed successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "Alert check failed: " + e.getMessage()
            ));
        }
    }
}
