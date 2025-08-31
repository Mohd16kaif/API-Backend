package com.apishield.repository;

import com.apishield.model.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {

    List<SubscriptionPlan> findByIsActiveTrueOrderByDisplayOrderAsc();

    Optional<SubscriptionPlan> findByNameIgnoreCase(String name);

    boolean existsByName(String name);

    @Query("SELECT sp FROM SubscriptionPlan sp WHERE sp.isActive = true AND sp.priceUsd > 0 ORDER BY sp.priceUsd ASC")
    List<SubscriptionPlan> findActivePaidPlansOrderByPrice();

    @Query("SELECT sp FROM SubscriptionPlan sp WHERE sp.isActive = true AND (sp.priceUsd = 0 OR sp.priceInr = 0)")
    Optional<SubscriptionPlan> findFreePlan();

    long countByIsActive(Boolean isActive);
}
