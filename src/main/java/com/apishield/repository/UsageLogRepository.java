package com.apishield.repository;

import com.apishield.model.ApiService;
import com.apishield.model.UsageLog;
import com.apishield.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsageLogRepository extends JpaRepository<UsageLog, Long> {

    // Basic queries
    List<UsageLog> findByApiServiceOrderByLogDateDesc(ApiService apiService);

    Page<UsageLog> findByApiServiceOrderByLogDateDesc(ApiService apiService, Pageable pageable);

    Optional<UsageLog> findByApiServiceAndLogDate(ApiService apiService, LocalDate logDate);

    List<UsageLog> findByApiServiceAndLogDateBetweenOrderByLogDateDesc(
            ApiService apiService, LocalDate startDate, LocalDate endDate);

    // Analytics queries
    @Query("SELECT ul FROM UsageLog ul WHERE ul.apiService.user = :user ORDER BY ul.logDate DESC")
    List<UsageLog> findByUserOrderByLogDateDesc(@Param("user") User user);

    @Query("SELECT ul FROM UsageLog ul WHERE ul.apiService.user = :user AND ul.logDate BETWEEN :startDate AND :endDate ORDER BY ul.logDate DESC")
    List<UsageLog> findByUserAndDateRangeOrderByLogDateDesc(
            @Param("user") User user, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(ul.requestsMade) FROM UsageLog ul WHERE ul.apiService.user = :user")
    Optional<Long> getTotalRequestsByUser(@Param("user") User user);

    @Query("SELECT SUM(ul.successCount) FROM UsageLog ul WHERE ul.apiService.user = :user")
    Optional<Long> getTotalSuccessCountByUser(@Param("user") User user);

    @Query("SELECT SUM(ul.errorCount) FROM UsageLog ul WHERE ul.apiService.user = :user")
    Optional<Long> getTotalErrorCountByUser(@Param("user") User user);

    @Query("SELECT ul.peakHour, COUNT(ul) as frequency FROM UsageLog ul WHERE ul.apiService.user = :user GROUP BY ul.peakHour ORDER BY frequency DESC")
    List<Object[]> findMostCommonPeakHoursByUser(@Param("user") User user);

    @Query("SELECT ul.apiService.id, ul.apiService.name, SUM(ul.requestsMade) as totalRequests " +
            "FROM UsageLog ul WHERE ul.apiService.user = :user " +
            "GROUP BY ul.apiService.id, ul.apiService.name " +
            "ORDER BY totalRequests DESC")
    List<Object[]> findTopApiServicesByUser(@Param("user") User user, Pageable pageable);

    @Query("SELECT ul FROM UsageLog ul WHERE ul.apiService.user = :user AND ul.logDate >= :date ORDER BY ul.requestsMade DESC")
    List<UsageLog> findRecentHighUsageLogs(@Param("user") User user, @Param("date") LocalDate date, Pageable pageable);

    @Query("SELECT AVG(CAST(ul.successCount AS DOUBLE) / ul.requestsMade * 100) FROM UsageLog ul WHERE ul.apiService.user = :user AND ul.requestsMade > 0")
    Optional<Double> getAverageSuccessRateByUser(@Param("user") User user);

    @Query("SELECT AVG(CAST(ul.errorCount AS DOUBLE) / ul.requestsMade * 100) FROM UsageLog ul WHERE ul.apiService.user = :user AND ul.requestsMade > 0")
    Optional<Double> getAverageErrorRateByUser(@Param("user") User user);

    @Query("SELECT ul FROM UsageLog ul WHERE ul.apiService.user = :user AND CAST(ul.errorCount AS DOUBLE) / ul.requestsMade > :threshold AND ul.requestsMade > 0")
    List<UsageLog> findHighErrorRateLogs(@Param("user") User user, @Param("threshold") double threshold);

    @Query("SELECT DATE(ul.logDate) as date, SUM(ul.requestsMade) as totalRequests " +
            "FROM UsageLog ul WHERE ul.apiService.user = :user AND ul.logDate >= :startDate " +
            "GROUP BY DATE(ul.logDate) ORDER BY date DESC")
    List<Object[]> getDailyUsageTrend(@Param("user") User user, @Param("startDate") LocalDate startDate);

    // Spike detection
    @Query("SELECT ul FROM UsageLog ul WHERE ul.apiService = :apiService AND ul.logDate = :yesterday")
    Optional<UsageLog> findYesterdayLog(@Param("apiService") ApiService apiService, @Param("yesterday") LocalDate yesterday);

    @Query("SELECT ul FROM UsageLog ul WHERE ul.apiService = :apiService AND ul.logDate = :dayBeforeYesterday")
    Optional<UsageLog> findDayBeforeYesterdayLog(@Param("apiService") ApiService apiService, @Param("dayBeforeYesterday") LocalDate dayBeforeYesterday);
}
