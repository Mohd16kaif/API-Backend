package com.apishield.controller;

import com.apishield.dto.service.ApiServiceRequest;
import com.apishield.dto.service.ApiServiceResponse;
import com.apishield.dto.service.ApiServiceUpdateRequest;
import com.apishield.model.User;
import com.apishield.service.ApiServiceService;
import com.apishield.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/services")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "API Services", description = "API Services management APIs")
public class ApiServiceController {

    private final ApiServiceService apiServiceService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "Create a new API service")
    public ResponseEntity<ApiServiceResponse> createApiService(
            Authentication authentication,
            @Valid @RequestBody ApiServiceRequest request) {
        User user = userService.getCurrentUserEntity(authentication);
        ApiServiceResponse response = apiServiceService.createApiService(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Get all API services for the current user")
    public ResponseEntity<List<ApiServiceResponse>> getAllApiServices(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean paginated,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        User user = userService.getCurrentUserEntity(authentication);

        if (paginated) {
            Pageable pageable = PageRequest.of(page, size);
            Page<ApiServiceResponse> services = apiServiceService.getAllApiServices(user, pageable);
            return ResponseEntity.ok()
                    .header("X-Total-Count", String.valueOf(services.getTotalElements()))
                    .header("X-Total-Pages", String.valueOf(services.getTotalPages()))
                    .body(services.getContent());
        } else {
            List<ApiServiceResponse> services = apiServiceService.getAllApiServices(user);
            return ResponseEntity.ok(services);
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get API service by ID")
    public ResponseEntity<ApiServiceResponse> getApiServiceById(
            Authentication authentication,
            @Parameter(description = "API Service ID") @PathVariable Long id) {
        User user = userService.getCurrentUserEntity(authentication);
        ApiServiceResponse response = apiServiceService.getApiServiceById(user, id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update API service")
    public ResponseEntity<ApiServiceResponse> updateApiService(
            Authentication authentication,
            @Parameter(description = "API Service ID") @PathVariable Long id,
            @Valid @RequestBody ApiServiceUpdateRequest request) {
        User user = userService.getCurrentUserEntity(authentication);
        ApiServiceResponse response = apiServiceService.updateApiService(user, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete API service")
    public ResponseEntity<Void> deleteApiService(
            Authentication authentication,
            @Parameter(description = "API Service ID") @PathVariable Long id) {
        User user = userService.getCurrentUserEntity(authentication);
        apiServiceService.deleteApiService(user, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/usage")
    @Operation(summary = "Update usage count for API service")
    public ResponseEntity<ApiServiceResponse> updateUsage(
            Authentication authentication,
            @Parameter(description = "API Service ID") @PathVariable Long id,
            @RequestBody Map<String, Double> request) {
        User user = userService.getCurrentUserEntity(authentication);
        Double newUsageCount = request.get("usageCount");
        if (newUsageCount == null) {
            return ResponseEntity.badRequest().build();
        }
        ApiServiceResponse response = apiServiceService.updateUsage(user, id, newUsageCount);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @Operation(summary = "Get API services statistics")
    public ResponseEntity<Map<String, Object>> getApiServiceStats(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);
        long totalServices = apiServiceService.getApiServiceCount(user);
        Double totalSpent = apiServiceService.getTotalSpentByUser(user);
        List<ApiServiceResponse> criticalServices = apiServiceService.getServicesOverBudgetThreshold(user, 90.0);
        List<ApiServiceResponse> warningServices = apiServiceService.getServicesOverBudgetThreshold(user, 75.0);

        Map<String, Object> stats = Map.of(
                "totalServices", totalServices,
                "totalSpent", totalSpent,
                "criticalServices", criticalServices.size(),
                "warningServices", warningServices.size() - criticalServices.size(),
                "healthyServices", totalServices - warningServices.size()
        );

        return ResponseEntity.ok(stats);
    }
}
