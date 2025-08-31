package com.apishield.repository;

import com.apishield.model.User;
import com.apishield.model.UserSubscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {

    List<UserSubscription> findByUserOrderByCreatedAtDesc(User user);

    Page<UserSubscription> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    Optional<UserSubscription> findFirstByUserAndStatusOrderByCreatedAtDesc(User user, UserSubscription.Status status);

    @Query("SELECT us FROM UserSubscription us WHERE us.user = :user AND us.status = 'ACTIVE' AND (us.endDate IS NULL OR us.endDate > CURRENT_DATE)")
    Optional<UserSubscription> findActiveSubscription(@Param("user") User user);

    @Query("SELECT us FROM UserSubscription us WHERE us.status = 'ACTIVE' AND us.endDate = :date")
    List<UserSubscription> findSubscriptionsExpiringOn(@Param("date") LocalDate date);

    @Query("SELECT us FROM UserSubscription us WHERE us.status = 'ACTIVE' AND us.endDate BETWEEN :startDate AND :endDate")
    List<UserSubscription> findSubscriptionsExpiringBetween(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT COUNT(us) FROM UserSubscription us WHERE us.status = 'ACTIVE'")
    long countActiveSubscriptions();

    @Query("SELECT COUNT(us) FROM UserSubscription us WHERE us.plan.id = :planId AND us.status = 'ACTIVE'")
    long countActiveSubscriptionsByPlan(@Param("planId") Long planId);

    @Query("SELECT us.plan.name, COUNT(us) FROM UserSubscription us WHERE us.status = 'ACTIVE' GROUP BY us.plan.name")
    List<Object[]> getActiveSubscriptionStatsByPlan();

    @Query("SELECT SUM(us.amountPaid) FROM UserSubscription us WHERE us.createdAt >= :since")
    Optional<Double> getTotalRevenueSince(@Param("since") LocalDateTime since);

    @Query("SELECT us FROM UserSubscription us WHERE us.user = :user AND us.status IN ('ACTIVE', 'EXPIRED') ORDER BY us.createdAt DESC")
    List<UserSubscription> findBillingHistory(@Param("user") User user);

    boolean existsByUserAndStatus(User user, UserSubscription.Status status);
}
