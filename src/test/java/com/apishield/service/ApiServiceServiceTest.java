package com.apishield.service;

import com.apishield.dto.service.ApiServiceRequest;
import com.apishield.dto.service.ApiServiceResponse;
import com.apishield.exception.BadRequestException;
import com.apishield.exception.ResourceNotFoundException;
import com.apishield.model.ApiService;
import com.apishield.model.User;
import com.apishield.repository.ApiServiceRepository;
import com.apishield.util.BudgetCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiServiceServiceTest {

    @Mock
    private ApiServiceRepository apiServiceRepository;

    @Mock
    private BudgetCalculator budgetCalculator;

    @InjectMocks
    private ApiServiceService apiServiceService;

    private User testUser;
    private ApiService testApiService;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .username("testuser") // Added missing username field
                .email("test@example.com")
                .role(User.Role.USER)
                .currency(User.Currency.USD) // Fixed: Use correct field name
                .build();

        testApiService = ApiService.builder()
                .id(1L)
                .user(testUser)
                .name("Test API")
                .endpointUrl("https://api.test.com")
                .budget(100.0)
                .costPerUnit(0.01)
                .usageCount(1000.0)
                .build();
    }

    @Test
    void testCreateApiService_Success() {
        ApiServiceRequest request = new ApiServiceRequest();
        request.setName("OpenAI API");
        request.setEndpointUrl("https://api.openai.com");
        request.setBudget(200.0);
        request.setCostPerUnit(0.02);
        request.setUsageCount(500.0);

        when(apiServiceRepository.existsByNameAndUser(request.getName(), testUser)).thenReturn(false);
        when(apiServiceRepository.save(any(ApiService.class))).thenReturn(testApiService);
        when(budgetCalculator.calculateUtilizationPercentage(any())).thenReturn(10.0);
        when(budgetCalculator.calculateRemainingBudget(any())).thenReturn(90.0);
        when(budgetCalculator.calculateTotalSpent(any())).thenReturn(10.0);
        when(budgetCalculator.determineStatus(10.0)).thenReturn("healthy");
        when(budgetCalculator.getCurrencySymbol(User.Currency.USD)).thenReturn("$");

        ApiServiceResponse response = apiServiceService.createApiService(testUser, request);

        assertNotNull(response);
        assertEquals(testApiService.getId(), response.getId());
        verify(apiServiceRepository).save(any(ApiService.class));
    }

    @Test
    void testCreateApiService_DuplicateName() {
        ApiServiceRequest request = new ApiServiceRequest();
        request.setName("Duplicate API");
        request.setEndpointUrl("https://api.duplicate.com");
        request.setBudget(100.0);
        request.setCostPerUnit(0.01);
        request.setUsageCount(0.0);

        when(apiServiceRepository.existsByNameAndUser(request.getName(), testUser)).thenReturn(true);

        assertThrows(BadRequestException.class, () ->
                apiServiceService.createApiService(testUser, request));

        verify(apiServiceRepository, never()).save(any());
    }

    @Test
    void testGetAllApiServices() {
        List<ApiService> mockServices = Arrays.asList(testApiService);
        when(apiServiceRepository.findByUserOrderByCreatedAtDesc(testUser)).thenReturn(mockServices);
        when(budgetCalculator.calculateUtilizationPercentage(any())).thenReturn(10.0);
        when(budgetCalculator.calculateRemainingBudget(any())).thenReturn(90.0);
        when(budgetCalculator.calculateTotalSpent(any())).thenReturn(10.0);
        when(budgetCalculator.determineStatus(10.0)).thenReturn("healthy");
        when(budgetCalculator.getCurrencySymbol(User.Currency.USD)).thenReturn("$");

        List<ApiServiceResponse> responses = apiServiceService.getAllApiServices(testUser);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(testApiService.getId(), responses.get(0).getId());
    }

    @Test
    void testGetApiServiceById_NotFound() {
        when(apiServiceRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                apiServiceService.getApiServiceById(testUser, 1L));
    }

    @Test
    void testDeleteApiService_Success() {
        when(apiServiceRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testApiService));

        assertDoesNotThrow(() -> apiServiceService.deleteApiService(testUser, 1L));

        verify(apiServiceRepository).delete(testApiService);
    }

    @Test
    void testUpdateUsage() {
        when(apiServiceRepository.findByIdAndUser(1L, testUser)).thenReturn(Optional.of(testApiService));
        when(apiServiceRepository.save(any(ApiService.class))).thenReturn(testApiService);
        when(budgetCalculator.calculateUtilizationPercentage(any())).thenReturn(20.0);
        when(budgetCalculator.calculateRemainingBudget(any())).thenReturn(80.0);
        when(budgetCalculator.calculateTotalSpent(any())).thenReturn(20.0);
        when(budgetCalculator.determineStatus(20.0)).thenReturn("healthy");
        when(budgetCalculator.getCurrencySymbol(User.Currency.USD)).thenReturn("$");

        ApiServiceResponse response = apiServiceService.updateUsage(testUser, 1L, 2000.0);

        assertNotNull(response);
        verify(apiServiceRepository).save(testApiService);
        assertEquals(2000.0, testApiService.getUsageCount());
    }
}