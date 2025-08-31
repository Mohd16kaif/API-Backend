package com.apishield.repository;

import com.apishield.model.CurrencyRate;
import com.apishield.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, Long> {

    // Add this method to fix the test compilation error
    Optional<CurrencyRate> findByFromCurrencyAndToCurrency(User.Currency fromCurrency, User.Currency toCurrency);

    Optional<CurrencyRate> findByFromCurrencyAndToCurrencyAndIsActive(
            User.Currency fromCurrency, User.Currency toCurrency, Boolean isActive);

    @Query("SELECT cr FROM CurrencyRate cr WHERE cr.fromCurrency = :from AND cr.toCurrency = :to AND cr.isActive = true ORDER BY cr.updatedAt DESC")
    Optional<CurrencyRate> findLatestRate(@Param("from") User.Currency fromCurrency, @Param("to") User.Currency toCurrency);

    @Query("SELECT cr FROM CurrencyRate cr WHERE cr.isActive = true ORDER BY cr.updatedAt DESC")
    List<CurrencyRate> findAllActiveRates();

    @Query("SELECT cr FROM CurrencyRate cr WHERE cr.updatedAt < :cutoff AND cr.isActive = true")
    List<CurrencyRate> findStaleRates(@Param("cutoff") LocalDateTime cutoff);

    @Query("SELECT cr FROM CurrencyRate cr WHERE (cr.fromCurrency = :currency OR cr.toCurrency = :currency) AND cr.isActive = true")
    List<CurrencyRate> findRatesForCurrency(@Param("currency") User.Currency currency);

    List<CurrencyRate> findBySourceAndIsActive(String source, Boolean isActive);

    @Query("SELECT AVG(cr.rate) FROM CurrencyRate cr WHERE cr.fromCurrency = :from AND cr.toCurrency = :to AND cr.updatedAt >= :since")
    Optional<Double> getAverageRate(@Param("from") User.Currency fromCurrency,
                                    @Param("to") User.Currency toCurrency,
                                    @Param("since") LocalDateTime since);
}