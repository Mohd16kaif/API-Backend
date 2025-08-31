package com.apishield.repository;

import com.apishield.model.Alert;
import com.apishield.model.ApiService;
import com.apishield.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByUserOrderByCreatedAtDesc(User user);

    Page<Alert> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<Alert> findByUserAndIsResolvedOrderByCreatedAtDesc(User user, Boolean isResolved);

    List<Alert> findByApiServiceOrderByCreatedAtDesc(ApiService apiService);

    @Query("SELECT a FROM Alert a WHERE a.user = :user AND a.severity = :severity ORDER BY a.createdAt DESC")
    List<Alert> findByUserAndSeverity(@Param("user") User user, @Param("severity") Alert.Severity severity);

    @Query("SELECT a FROM Alert a WHERE a.user = :user AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<Alert> findRecentAlerts(@Param("user") User user, @Param("since") LocalDateTime since);

    // UPDATED: Include null values as unresolved
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.user = :user AND (a.isResolved = false OR a.isResolved IS NULL)")
    long countUnresolvedByUser(@Param("user") User user);

    // UPDATED: Include null values as unresolved
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.user = :user AND a.severity = :severity AND (a.isResolved = false OR a.isResolved IS NULL)")
    long countUnresolvedBySeverity(@Param("user") User user, @Param("severity") Alert.Severity severity);

    @Query("SELECT a FROM Alert a WHERE a.notificationSent = false AND a.createdAt >= :cutoff ORDER BY a.createdAt ASC")
    List<Alert> findUnsentNotifications(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT a FROM Alert a WHERE a.apiService = :apiService AND a.alertType = :alertType AND a.createdAt >= :since")
    List<Alert> findDuplicateAlerts(@Param("apiService") ApiService apiService,
                                    @Param("alertType") Alert.AlertType alertType,
                                    @Param("since") LocalDateTime since);

    @Query("SELECT a.alertType, COUNT(a) FROM Alert a WHERE a.user = :user AND a.createdAt >= :since GROUP BY a.alertType")
    List<Object[]> getAlertStatsByType(@Param("user") User user, @Param("since") LocalDateTime since);

    @Query("SELECT DATE(a.createdAt), COUNT(a) FROM Alert a WHERE a.user = :user AND a.createdAt >= :since GROUP BY DATE(a.createdAt) ORDER BY DATE(a.createdAt) DESC")
    List<Object[]> getDailyAlertCounts(@Param("user") User user, @Param("since") LocalDateTime since);
}