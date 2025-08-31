package com.apishield.controller;

import com.apishield.dto.settings.CurrencyConversionRequest;
import com.apishield.dto.settings.UserSettingsRequest;
import com.apishield.model.*;
import com.apishield.repository.*;
import com.apishield.security.JwtTokenUtil;
import com.apishield.security.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSettingsRepository settingsRepository;

    @Autowired
    private CurrencyRateRepository currencyRateRepository;

    @Autowired(required = false)
    private BudgetRepository budgetRepository;

    @Autowired(required = false)
    private ApiServiceRepository apiServiceRepository;

    @Autowired(required = false)
    private UsageLogRepository usageLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private String jwtToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up all related tables in proper order
        if (usageLogRepository != null) usageLogRepository.deleteAll();
        if (apiServiceRepository != null) apiServiceRepository.deleteAll();
        if (budgetRepository != null) budgetRepository.deleteAll();
        settingsRepository.deleteAll();
        currencyRateRepository.deleteAll();
        userRepository.deleteAll();

        // Create test user
        testUser = User.builder()
                .name("User Test User")
                .email("user@example.com")
                .username("usertestuser")
                .password(passwordEncoder.encode("password123"))
                .role(User.Role.USER)
                .currency(User.Currency.USD)
                .build();
        testUser = userRepository.save(testUser);

        // Create currency rates for conversion tests (check if exists first)
        createCurrencyRateIfNotExists(User.Currency.USD, User.Currency.INR, 83.0);
        createCurrencyRateIfNotExists(User.Currency.INR, User.Currency.USD, 0.012048);

        // Create initial user settings
        UserSettings defaultSettings = UserSettings.builder()
                .user(testUser)
                .currencyPreference(User.Currency.USD)
                .timezone("UTC")
                .dateFormat("MM/dd/yyyy")
                .timeFormat(UserSettings.TimeFormat.TWELVE_HOUR)
                .theme(UserSettings.Theme.LIGHT)
                .language("en")
                .emailNotifications(true)
                .budgetAlerts(true)
                .itemsPerPage(20)
                .autoRefreshDashboard(true)
                .build();
        settingsRepository.save(defaultSettings);

        // Generate JWT token
        UserPrincipal userPrincipal = UserPrincipal.create(testUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                userPrincipal.getAuthorities()
        );
        jwtToken = jwtTokenUtil.generateJwtToken(authentication);
    }

    private void createCurrencyRateIfNotExists(User.Currency from, User.Currency to, double rate) {
        // Use a simple existence check or try-catch to avoid duplicates
        try {
            Optional<CurrencyRate> existing = currencyRateRepository.findByFromCurrencyAndToCurrency(from, to);
            if (existing.isEmpty()) {
                CurrencyRate currencyRate = CurrencyRate.builder()
                        .fromCurrency(from)
                        .toCurrency(to)
                        .rate(rate)
                        .source("TEST")
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                currencyRateRepository.save(currencyRate);
            }
        } catch (Exception e) {
            // If constraint violation occurs, the record already exists, so ignore
        }
    }

    @Test
    void testUpdateCurrency() throws Exception {
        mockMvc.perform(put("/api/user/currency")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\": \"INR\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateCurrencyWithInvalidValue() throws Exception {
        mockMvc.perform(put("/api/user/currency")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\": \"INVALID_CURRENCY\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGetUserProfile() throws Exception {
        mockMvc.perform(get("/api/user/profile")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("User Test User"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.username").value("usertestuser"));
    }

    @Test
    void testConvertCurrency() throws Exception {
        CurrencyConversionRequest request = new CurrencyConversionRequest();
        request.setAmount(100.0);
        request.setFromCurrency(User.Currency.USD);
        request.setToCurrency(User.Currency.INR);

        mockMvc.perform(post("/api/user/currency/convert")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalAmount").value(100.0))
                .andExpect(jsonPath("$.fromCurrency").value("USD"))
                .andExpect(jsonPath("$.toCurrency").value("INR"));
    }
}