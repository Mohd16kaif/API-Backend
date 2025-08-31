package com.apishield.repository;

import com.apishield.model.ApiService;
import com.apishield.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiServiceRepository extends JpaRepository<ApiService, Long> {

    // Original methods from your repository
    List<ApiService> findByUserOrderByCreatedAtDesc(User user);

    Page<ApiService> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Optional<ApiService> findByIdAndUser(Long id, User user);

    boolean existsByNameAndUser(String name, User user);

    boolean existsByNameAndUserAndIdNot(String name, User user, Long id);

    long countByUser(User user);

    @Query("SELECT SUM(a.usageCount * a.costPerUnit) FROM ApiService a WHERE a.user = :user")
    Optional<Double> getTotalSpentByUser(@Param("user") User user);

    @Query("SELECT a FROM ApiService a WHERE a.user = :user AND " +
            "((a.usageCount * a.costPerUnit) / a.budget) * 100 >= :threshold")
    List<ApiService> findServicesOverBudgetThreshold(@Param("user") User user, @Param("threshold") double threshold);

    // ADDED: Methods needed for health indicator

    /**
     * Count services by active status
     * @param isActive true for active services, false for inactive
     * @return count of services matching the active status
     */
    long countByIsActive(boolean isActive);

    /**
     * Count services by user and active status
     * @param user the user to filter by
     * @param isActive true for active services, false for inactive
     * @return count of user's services matching the active status
     */
    long countByUserAndIsActive(User user, boolean isActive);

    /**
     * Find all services by active status
     * @param isActive true for active services, false for inactive
     * @return list of services matching the active status
     */
    List<ApiService> findByIsActive(boolean isActive);

    /**
     * Find services by user and active status, ordered by creation date
     * @param user the user to filter by
     * @param isActive true for active services, false for inactive
     * @return list of user's services matching the active status
     */
    List<ApiService> findByUserAndIsActiveOrderByCreatedAtDesc(User user, boolean isActive);

    // ADDED: Additional health-related queries using @Query annotation

    /**
     * Count all active services across all users
     * @return count of active services
     */
    @Query("SELECT COUNT(a) FROM ApiService a WHERE a.isActive = true")
    long countActiveServices();

    /**
     * Count all inactive services across all users
     * @return count of inactive services
     */
    @Query("SELECT COUNT(a) FROM ApiService a WHERE a.isActive = false")
    long countInactiveServices();

    /**
     * Get average usage count for active services
     * @return average usage count or null if no services exist
     */
    @Query("SELECT AVG(a.usageCount) FROM ApiService a WHERE a.isActive = true")
    Optional<Double> getAverageUsageForActiveServices();

    /**
     * Find services that haven't been used recently (usage count is 0)
     * @return list of unused services
     */
    @Query("SELECT a FROM ApiService a WHERE a.isActive = true AND a.usageCount = 0")
    List<ApiService> findUnusedActiveServices();

    /**
     * Count services by active status for a specific user
     * @param userId the user ID to filter by
     * @param isActive true for active services, false for inactive
     * @return count of services matching criteria
     */
    @Query("SELECT COUNT(a) FROM ApiService a WHERE a.user.id = :userId AND a.isActive = :isActive")
    long countByUserIdAndIsActive(@Param("userId") Long userId, @Param("isActive") boolean isActive);
}