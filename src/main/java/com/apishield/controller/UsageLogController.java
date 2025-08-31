package com.apishield.controller;

import java.util.Map;
import com.apishield.dto.usage.*;
import com.apishield.model.User;
import com.apishield.service.ApiServiceService;
import com.apishield.service.UsageLogService;
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
@RequestMapping("/api/usage")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Usage Analytics", description = "API usage tracking and analytics")
public class UsageLogController {

    private final UsageLogService usageLogService;
    private final UserService userService;
    private final ApiServiceService apiServiceService;

    @PostMapping
    @Operation(summary = "Create a new usage log entry")
    public ResponseEntity<UsageLogResponse> createUsageLog(
            Authentication authentication,
            @Valid @RequestBody UsageLogRequest request) {
        User user = userService.getCurrentUserEntity(authentication);
        UsageLogResponse response = usageLogService.createUsageLog(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get overall usage analytics")
    public ResponseEntity<Map<String, Object>> getOverallUsageAnalytics(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);

        long totalServices = apiServiceService.getApiServiceCount(user);

        Map<String, Object> analytics = Map.of(
                "totalServices", totalServices
        );

        return ResponseEntity.ok(analytics);
    }

    @GetMapping("/{apiServiceId}")
    @Operation(summary = "Get usage logs for a specific API service")
    public ResponseEntity<List<UsageLogResponse>> getUsageLogsByApiService(
            Authentication authentication,
            @Parameter(description = "API Service ID") @PathVariable Long apiServiceId) {
        User user = userService.getCurrentUserEntity(authentication);
        List<UsageLogResponse> logs = usageLogService.getUsageLogsByApiService(user, apiServiceId);
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/top-api")
    @Operation(summary = "Get the top API service by usage")
    public ResponseEntity<TopApiResponse> getTopApi(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);
        TopApiResponse topApi = usageLogService.getTopApiByUser(user);

        if (topApi == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(topApi);
    }



    @GetMapping("/summary")
    @Operation(summary = "Get comprehensive usage summary and analytics")
    public ResponseEntity<UsageSummaryResponse> getUsageSummary(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);
        UsageSummaryResponse summary = usageLogService.getUsageSummary(user);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/analytics")
    @Operation(summary = "Get detailed usage analytics with insights and recommendations")
    public ResponseEntity<UsageAnalyticsResponse> getUsageAnalytics(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);
        UsageAnalyticsResponse analytics = usageLogService.getUsageAnalytics(user);
        return ResponseEntity.ok(analytics);
    }
}
