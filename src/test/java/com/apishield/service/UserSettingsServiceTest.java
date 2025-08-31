package com.apishield.service;

import com.apishield.dto.settings.UserSettingsRequest;
import com.apishield.dto.settings.UserSettingsResponse;
import com.apishield.model.User;
import com.apishield.model.UserSettings;
import com.apishield.repository.UserSettingsRepository;
import com.apishield.util.CurrencyConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

    @Mock
    private UserSettingsRepository settingsRepository;

    @Mock
    private CurrencyConverter currencyConverter;

    @InjectMocks
    private UserSettingsService userSettingsService;

    private User testUser;
    private UserSettings testSettings;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .email("test@example.com")
                .currency(User.Currency.USD)
                .build();

        testSettings = UserSettings.builder()
                .id(1L)
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
                .build();
    }

    @Test
    void testGetSettings_ExistingSettings() {
        when(settingsRepository.findByUser(testUser)).thenReturn(Optional.of(testSettings));
        when(currencyConverter.getCurrencySymbol(User.Currency.USD)).thenReturn("$");

        UserSettingsResponse response = userSettingsService.getSettings(testUser);

        assertNotNull(response);
        assertEquals(User.Currency.USD, response.getCurrencyPreference());
        assertEquals("$", response.getCurrencySymbol());
        assertEquals("UTC", response.getTimezone());
        assertEquals(20, response.getItemsPerPage());
        assertTrue(response.getEmailNotifications());
        assertNotNull(response.getAvailableOptions());
    }

    @Test
    void testGetSettings_NoExistingSettings() {
        when(settingsRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(currencyConverter.getCurrencySymbol(User.Currency.USD)).thenReturn("$");

        UserSettingsResponse response = userSettingsService.getSettings(testUser);

        assertNotNull(response);
        assertEquals(User.Currency.USD, response.getCurrencyPreference());
        assertEquals("UTC", response.getTimezone());
        assertEquals(20, response.getItemsPerPage());
    }

    @Test
    void testUpdateSettings() {
        UserSettingsRequest request = new UserSettingsRequest();
        request.setCurrencyPreference(User.Currency.INR);
        request.setTimezone("Asia/Kolkata");
        request.setTheme(UserSettings.Theme.DARK);
        request.setEmailNotifications(false);
        request.setItemsPerPage(50);

        when(settingsRepository.findByUser(testUser)).thenReturn(Optional.of(testSettings));
        when(settingsRepository.save(any(UserSettings.class))).thenReturn(testSettings);
        when(currencyConverter.getCurrencySymbol(User.Currency.INR)).thenReturn("₹");

        UserSettingsResponse response = userSettingsService.updateSettings(testUser, request);

        assertNotNull(response);
        assertEquals(User.Currency.INR, testUser.getCurrencyPreference());
        verify(settingsRepository).save(any(UserSettings.class));
    }

    @Test
    void testUpdateCurrencyPreference() {
        when(settingsRepository.findByUser(testUser)).thenReturn(Optional.of(testSettings));
        when(settingsRepository.save(any(UserSettings.class))).thenReturn(testSettings);
        when(currencyConverter.getCurrencySymbol(User.Currency.INR)).thenReturn("₹");

        UserSettingsResponse response = userSettingsService.updateCurrencyPreference(testUser, User.Currency.INR);

        assertNotNull(response);
        assertEquals(User.Currency.INR, testUser.getCurrencyPreference());
        verify(settingsRepository).save(any(UserSettings.class));
    }

    @Test
    void testResetToDefaults() {
        testSettings.setCurrencyPreference(User.Currency.INR);
        testSettings.setTheme(UserSettings.Theme.DARK);
        testSettings.setEmailNotifications(false);

        when(settingsRepository.findByUser(testUser)).thenReturn(Optional.of(testSettings));
        when(settingsRepository.save(any(UserSettings.class))).thenReturn(testSettings);
        when(currencyConverter.getCurrencySymbol(User.Currency.USD)).thenReturn("$");

        UserSettingsResponse response = userSettingsService.resetToDefaults(testUser);

        assertNotNull(response);
        assertEquals(User.Currency.USD, testSettings.getCurrencyPreference());
        assertEquals(UserSettings.Theme.LIGHT, testSettings.getTheme());
        assertTrue(testSettings.getEmailNotifications());
        verify(settingsRepository).save(testSettings);
    }
}
