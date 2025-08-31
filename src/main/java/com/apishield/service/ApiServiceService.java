package com.apishield.service;

import com.apishield.dto.service.ApiServiceRequest;
import com.apishield.dto.service.ApiServiceResponse;
import com.apishield.dto.service.ApiServiceUpdateRequest;
import com.apishield.exception.BadRequestException;
import com.apishield.exception.ResourceNotFoundException;
import com.apishield.model.ApiService;
import com.apishield.model.User;
import com.apishield.repository.ApiServiceRepository;
import com.apishield.util.BudgetCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiServiceService {

    private final ApiServiceRepository apiServiceRepository;
    private final BudgetCalculator budgetCalculator;

    @Transactional
    public ApiServiceResponse createApiService(User user, ApiServiceRequest request) {
        log.info("Creating API service for user: {} with name: {}", user.getEmail(), request.getName());

        // Check if API service with same name already exists for user
        if (apiServiceRepository.existsByNameAndUser(request.getName(), user)) {
            throw new BadRequestException("API service with name '" + request.getName() + "' already exists");
        }

        ApiService apiService = ApiService.builder()
                .user(user)
                .name(request.getName())
                .endpointUrl(request.getEndpointUrl())
                .budget(request.getBudget())
                .costPerUnit(request.getCostPerUnit())
                .usageCount(request.getUsageCount())
                .build();

        ApiService savedApiService = apiServiceRepository.save(apiService);
        log.info("Successfully created API service with ID: {}", savedApiService.getId());

        return mapToResponse(savedApiService, user);
    }

    @Transactional(readOnly = true)
    public List<ApiServiceResponse> getAllApiServices(User user) {
        log.info("Fetching all API services for user: {}", user.getEmail());

        List<ApiService> apiServices = apiServiceRepository.findByUserOrderByCreatedAtDesc(user);
        return apiServices.stream()
                .map(service -> mapToResponse(service, user))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ApiServiceResponse> getAllApiServices(User user, Pageable pageable) {
        log.info("Fetching paginated API services for user: {} with page: {}", user.getEmail(), pageable.getPageNumber());

        Page<ApiService> apiServices = apiServiceRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return apiServices.map(service -> mapToResponse(service, user));
    }

    @Transactional(readOnly = true)
    public ApiServiceResponse getApiServiceById(User user, Long id) {
        log.info("Fetching API service with ID: {} for user: {}", id, user.getEmail());

        ApiService apiService = apiServiceRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("API service not found with ID: " + id));

        return mapToResponse(apiService, user);
    }

    @Transactional
    public ApiServiceResponse updateApiService(User user, Long id, ApiServiceUpdateRequest request) {
        log.info("Updating API service with ID: {} for user: {}", id, user.getEmail());

        ApiService apiService = apiServiceRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("API service not found with ID: " + id));

        // Check for name uniqueness if name is being updated
        if (request.getName() != null && !request.getName().equals(apiService.getName())) {
            if (apiServiceRepository.existsByNameAndUserAndIdNot(request.getName(), user, id)) {
                throw new BadRequestException("API service with name '" + request.getName() + "' already exists");
            }
            apiService.setName(request.getName());
        }

        // Update other fields if provided
        if (request.getEndpointUrl() != null) {
            apiService.setEndpointUrl(request.getEndpointUrl());
        }
        if (request.getBudget() != null) {
            apiService.setBudget(request.getBudget());
        }
        if (request.getCostPerUnit() != null) {
            apiService.setCostPerUnit(request.getCostPerUnit());
        }
        if (request.getUsageCount() != null) {
            apiService.setUsageCount(request.getUsageCount());
        }

        ApiService updatedApiService = apiServiceRepository.save(apiService);
        log.info("Successfully updated API service with ID: {}", updatedApiService.getId());

        return mapToResponse(updatedApiService, user);
    }

    @Transactional
    public void deleteApiService(User user, Long id) {
        log.info("Deleting API service with ID: {} for user: {}", id, user.getEmail());

        ApiService apiService = apiServiceRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("API service not found with ID: " + id));

        apiServiceRepository.delete(apiService);
        log.info("Successfully deleted API service with ID: {}", id);
    }

    @Transactional
    public ApiServiceResponse updateUsage(User user, Long id, Double newUsageCount) {
        log.info("Updating usage for API service ID: {} to: {}", id, newUsageCount);

        ApiService apiService = apiServiceRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("API service not found with ID: " + id));

        apiService.setUsageCount(newUsageCount);
        ApiService updatedApiService = apiServiceRepository.save(apiService);

        return mapToResponse(updatedApiService, user);
    }

    @Transactional(readOnly = true)
    public long getApiServiceCount(User user) {
        return apiServiceRepository.countByUser(user);
    }

    @Transactional(readOnly = true)
    public Double getTotalSpentByUser(User user) {
        return apiServiceRepository.getTotalSpentByUser(user).orElse(0.0);
    }

    @Transactional(readOnly = true)
    public List<ApiServiceResponse> getServicesOverBudgetThreshold(User user, double threshold) {
        List<ApiService> services = apiServiceRepository.findServicesOverBudgetThreshold(user, threshold);
        return services.stream()
                .map(service -> mapToResponse(service, user))
                .collect(Collectors.toList());
    }

    private ApiServiceResponse mapToResponse(ApiService apiService, User user) {
        double utilizationPercentage = budgetCalculator.calculateUtilizationPercentage(apiService);
        double remainingBudget = budgetCalculator.calculateRemainingBudget(apiService);
        double totalSpent = budgetCalculator.calculateTotalSpent(apiService);
        String status = budgetCalculator.determineStatus(utilizationPercentage);
        String currencySymbol = budgetCalculator.getCurrencySymbol(user.getCurrencyPreference());

        return ApiServiceResponse.builder()
                .id(apiService.getId())
                .name(apiService.getName())
                .endpointUrl(apiService.getEndpointUrl())
                .budget(apiService.getBudget())
                .costPerUnit(apiService.getCostPerUnit())
                .usageCount(apiService.getUsageCount())
                .utilizationPercentage(Math.round(utilizationPercentage * 100.0) / 100.0)
                .remainingBudget(Math.round(remainingBudget * 100.0) / 100.0)
                .totalSpent(Math.round(totalSpent * 100.0) / 100.0)
                .status(status)
                .createdAt(apiService.getCreatedAt())
                .updatedAt(apiService.getUpdatedAt())
                .currencySymbol(currencySymbol)
                .budgetInUserCurrency(apiService.getBudget())
                .costPerUnitInUserCurrency(apiService.getCostPerUnit())
                .totalSpentInUserCurrency(totalSpent)
                .remainingBudgetInUserCurrency(remainingBudget)
                .build();
    }
}
