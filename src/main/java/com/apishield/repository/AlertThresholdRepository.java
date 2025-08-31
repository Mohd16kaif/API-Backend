package com.apishield.repository;

import com.apishield.model.AlertThreshold;
import com.apishield.model.ApiService;
import com.apishield.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertThresholdRepository extends JpaRepository<AlertThreshold, Long> {

    Optional<AlertThreshold> findByApiService(ApiService apiService);

    Optional<AlertThreshold> findByApiServiceAndIsEnabled(ApiService apiService, Boolean isEnabled);

    @Query("SELECT at FROM AlertThreshold at WHERE at.apiService.user = :user")
    List<AlertThreshold> findByUser(@Param("user") User user);

    @Query("SELECT at FROM AlertThreshold at WHERE at.apiService.user = :user AND at.isEnabled = true")
    List<AlertThreshold> findEnabledByUser(@Param("user") User user);

    @Query("SELECT at FROM AlertThreshold at WHERE at.isEnabled = true")
    List<AlertThreshold> findAllEnabled();

    @Query("SELECT COUNT(at) FROM AlertThreshold at WHERE at.apiService.user = :user AND at.isEnabled = true")
    long countEnabledByUser(@Param("user") User user);

    boolean existsByApiService(ApiService apiService);
}
