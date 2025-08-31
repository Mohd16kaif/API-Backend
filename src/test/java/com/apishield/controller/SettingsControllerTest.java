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
class SettingsControllerTest {

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    private String jwtToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        // Clean up in correct order
        settingsRepository.deleteAll();
        currencyRateRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .name("Settings Test User")
                .email("settings@example.com")
                .username("settingstestuser")
                .password(passwordEncoder.encode("password123"))
                .role(User.Role.USER)
                .currency(User.Currency.USD)
                .build();
        testUser = userRepository.save(testUser);

        // Check and create currency rates only if they don't exist
        createCurrencyRateIfNotExists(User.Currency.USD, User.Currency.INR, 83.0);
        createCurrencyRateIfNotExists(User.Currency.INR, User.Currency.USD, 0.012048);

        UserPrincipal userPrincipal = UserPrincipal.create(testUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                userPrincipal.getAuthorities()
        );
        jwtToken = jwtTokenUtil.generateJwtToken(authentication);
    }

    private void createCurrencyRateIfNotExists(User.Currency from, User.Currency to, double rate) {
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
    }

    @Test
    void testGetSettings_DefaultSettings() throws Exception {
        mockMvc.perform(get("/api/user/settings")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencyPreference").value("USD"))
                .andExpect(jsonPath("$.currencySymbol").value("$"))
                .andExpect(jsonPath("$.timezone").value("UTC"))
                .andExpect(jsonPath("$.dateFormat").value("MM/dd/yyyy"))
                .andExpect(jsonPath("$.timeFormat").value("TWELVE_HOUR"))
                .andExpect(jsonPath("$.theme").value("LIGHT"))
                .andExpect(jsonPath("$.language").value("en"))
                .andExpect(jsonPath("$.emailNotifications").value(true))
                .andExpect(jsonPath("$.budgetAlerts").value(true))
                .andExpect(jsonPath("$.itemsPerPage").value(20));
    }

    @Test
    void testUpdateSettings() throws Exception {
        UserSettingsRequest request = new UserSettingsRequest();
        request.setCurrencyPreference(User.Currency.INR);
        request.setTimezone("Asia/Kolkata");
        request.setDateFormat("dd/MM/yyyy");
        request.setTimeFormat(UserSettings.TimeFormat.TWENTY_FOUR_HOUR);
        request.setTheme(UserSettings.Theme.DARK);
        request.setLanguage("hi");
        request.setEmailNotifications(false);
        request.setItemsPerPage(50);
        request.setAutoRefreshDashboard(false);

        mockMvc.perform(put("/api/user/settings")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currencyPreference").value("INR"))
                .andExpect(jsonPath("$.timezone").value("Asia/Kolkata"))
                .andExpect(jsonPath("$.dateFormat").value("dd/MM/yyyy"));
    }

    @Test
    void testUpdateCurrencyPreference() throws Exception {
        mockMvc.perform(put("/api/user/currency")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\": \"INR\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void testUpdateCurrencyPreference_InvalidCurrency() throws Exception {
        mockMvc.perform(put("/api/user/currency")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currency\": \"INVALID\"}"))
                .andExpect(status().isBadRequest());
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

    @Test
    void testConvertCurrency_SameCurrency() throws Exception {
        CurrencyConversionRequest request = new CurrencyConversionRequest();
        request.setAmount(100.0);
        request.setFromCurrency(User.Currency.USD);
        request.setToCurrency(User.Currency.USD);

        mockMvc.perform(post("/api/user/currency/convert")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalAmount").value(100.0))
                .andExpect(jsonPath("$.convertedAmount").value(100.0))
                .andExpect(jsonPath("$.exchangeRate").value(1.0));
    }

    @Test
    void testGetExchangeRates() throws Exception {
        mockMvc.perform(get("/api/user/currency/rates")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rates").exists());
    }

    @Test
    void testResetSettings() throws Exception {
        // First, update some settings
        UserSettings settings = UserSettings.builder()
                .user(testUser)
                .currencyPreference(User.Currency.INR)
                .timezone("Asia/Kolkata")
                .theme(UserSettings.Theme.DARK)
                .emailNotifications(false)
                .itemsPerPage(100)
                .build();
        settingsRepository.save(settings);

        // Then reset to defaults
        mockMvc.perform(post("/api/user/settings/reset")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }

    @Test
    void testConvertCurrency_InvalidAmount() throws Exception {
        CurrencyConversionRequest request = new CurrencyConversionRequest();
        request.setAmount(-100.0); // Invalid negative amount
        request.setFromCurrency(User.Currency.USD);
        request.setToCurrency(User.Currency.INR);

        mockMvc.perform(post("/api/user/currency/convert")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}