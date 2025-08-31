package com.apishield.repository;

import com.apishield.model.Budget;
import com.apishield.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByUser(User user);

    boolean existsByUser(User user);

    @Query("SELECT b FROM Budget b WHERE b.user = :user")
    Optional<Budget> findBudgetByUser(@Param("user") User user);

    @Modifying
    @Query("UPDATE Budget b SET b.spentAmount = :spentAmount WHERE b.user = :user")
    int updateSpentAmount(@Param("user") User user, @Param("spentAmount") Double spentAmount);

    @Query("SELECT b FROM Budget b WHERE (b.spentAmount / b.monthlyBudget) * 100 >= :threshold")
    List<Budget> findBudgetsOverThreshold(@Param("threshold") double threshold);

    @Query("SELECT AVG(b.monthlyBudget) FROM Budget b")
    Optional<Double> getAverageMonthlyBudget();

    @Query("SELECT COUNT(b) FROM Budget b WHERE b.spentAmount > b.monthlyBudget")
    long countOverBudgetUsers();
}
