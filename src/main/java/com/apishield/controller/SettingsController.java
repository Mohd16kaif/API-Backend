package com.apishield.controller;

import com.apishield.dto.settings.*;
import com.apishield.model.User;
import com.apishield.service.CurrencyService;
import com.apishield.service.UserService;
import com.apishield.service.UserSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Settings", description = "User settings and currency management APIs")
public class SettingsController {

    private final UserSettingsService userSettingsService;
    private final CurrencyService currencyService;
    private final UserService userService;

    @GetMapping("/settings")
    @Operation(summary = "Get user settings")
    public ResponseEntity<UserSettingsResponse> getSettings(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);
        UserSettingsResponse settings = userSettingsService.getSettings(user);
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/settings")
    @Operation(summary = "Update user settings")
    public ResponseEntity<UserSettingsResponse> updateSettings(
            Authentication authentication,
            @Valid @RequestBody UserSettingsRequest request) {
        User user = userService.getCurrentUserEntity(authentication);
        UserSettingsResponse settings = userSettingsService.updateSettings(user, request);
        return ResponseEntity.ok(settings);
    }

    @PutMapping("/currency")
    @Operation(summary = "Update currency preference")
    public ResponseEntity<UserSettingsResponse> updateCurrency(
            Authentication authentication,
            @RequestBody Map<String, String> request) {
        User user = userService.getCurrentUserEntity(authentication);
        String currencyStr = request.get("currency");

        try {
            User.Currency currency = User.Currency.valueOf(currencyStr.toUpperCase());
            UserSettingsResponse settings = userSettingsService.updateCurrencyPreference(user, currency);
            return ResponseEntity.ok(settings);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/settings/reset")
    @Operation(summary = "Reset settings to defaults")
    public ResponseEntity<UserSettingsResponse> resetSettings(Authentication authentication) {
        User user = userService.getCurrentUserEntity(authentication);
        UserSettingsResponse settings = userSettingsService.resetToDefaults(user);
        return ResponseEntity.ok(settings);
    }

    @PostMapping("/currency/convert")
    @Operation(summary = "Convert currency amounts")
    public ResponseEntity<CurrencyConversionResponse> convertCurrency(
            @Valid @RequestBody CurrencyConversionRequest request) {
        CurrencyConversionResponse response = currencyService.convertCurrency(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/currency/rates")
    @Operation(summary = "Get all exchange rates")
    public ResponseEntity<Map<String, Object>> getExchangeRates() {
        Map<String, Object> rates = currencyService.getAllExchangeRates();
        return ResponseEntity.ok(rates);
    }

    @GetMapping("/currency/stats")
    @Operation(summary = "Get currency system statistics")
    public ResponseEntity<Map<String, Object>> getCurrencyStats() {
        Map<String, Object> stats = currencyService.getCurrencyStats();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/currency/refresh-rates")
    @Operation(summary = "Manually refresh exchange rates")
    public ResponseEntity<Map<String, String>> refreshExchangeRates() {
        try {
            currencyService.updateExchangeRates();
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "Exchange rates updated successfully"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Failed to update exchange rates: " + e.getMessage()
            ));
        }
    }
}
