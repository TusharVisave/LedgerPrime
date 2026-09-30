package com.expensetracker.repository;

import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.TransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByIdAndUserId(Long id, Long userId);

    /**
     * Filtered, paginated transaction search. The keyword pattern must already be
     * LIKE-escaped by the caller (%, _ and \ escaped, wrapped in %...%).
     */
    @Query("""
            SELECT t FROM Transaction t WHERE t.user.id = :userId
              AND (:type IS NULL OR t.type = :type)
              AND (:category IS NULL OR t.category = :category)
              AND (:minPaise IS NULL OR t.amountPaise >= :minPaise)
              AND (:maxPaise IS NULL OR t.amountPaise <= :maxPaise)
              AND (:startDate IS NULL OR t.date >= :startDate)
              AND (:endDate IS NULL OR t.date <= :endDate)
              AND (:keyword IS NULL OR LOWER(t.note) LIKE :keyword ESCAPE '\\'
                   OR LOWER(t.category) LIKE :keyword ESCAPE '\\'
                   OR EXISTS (SELECT tg FROM Transaction tx JOIN tx.tags tg
                              WHERE tx.id = t.id AND LOWER(tg) LIKE :keyword ESCAPE '\\'))
            """)
    Page<Transaction> search(@Param("userId") Long userId,
                             @Param("type") TransactionType type,
                             @Param("category") String category,
                             @Param("keyword") String keyword,
                             @Param("minPaise") Long minPaise,
                             @Param("maxPaise") Long maxPaise,
                             @Param("startDate") LocalDate startDate,
                             @Param("endDate") LocalDate endDate,
                             Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amountPaise), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :type AND t.date BETWEEN :start AND :end")
    long sumByUserIdAndTypeAndDateBetween(@Param("userId") Long userId,
                                          @Param("type") TransactionType type,
                                          @Param("start") LocalDate start,
                                          @Param("end") LocalDate end);

    @Query("SELECT COALESCE(SUM(t.amountPaise), 0) FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :type AND t.category = :category " +
           "AND t.date BETWEEN :start AND :end")
    long sumByUserIdAndTypeAndCategoryAndDateBetween(@Param("userId") Long userId,
                                                     @Param("type") TransactionType type,
                                                     @Param("category") String category,
                                                     @Param("start") LocalDate start,
                                                     @Param("end") LocalDate end);

    @Query("SELECT t.category AS name, SUM(t.amountPaise) AS total FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :type AND t.date BETWEEN :start AND :end " +
           "GROUP BY t.category ORDER BY SUM(t.amountPaise) DESC")
    List<CategoryTotal> sumByCategory(@Param("userId") Long userId,
                                      @Param("type") TransactionType type,
                                      @Param("start") LocalDate start,
                                      @Param("end") LocalDate end);

    @Query("SELECT YEAR(t.date) AS y, MONTH(t.date) AS m, t.type AS type, SUM(t.amountPaise) AS total " +
           "FROM Transaction t WHERE t.user.id = :userId " +
           "GROUP BY YEAR(t.date), MONTH(t.date), t.type " +
           "ORDER BY YEAR(t.date), MONTH(t.date)")
    List<MonthlyTotal> sumByMonth(@Param("userId") Long userId);

    @Query("SELECT DAY(t.date) AS day, SUM(t.amountPaise) AS total FROM Transaction t " +
           "WHERE t.user.id = :userId AND t.type = :type AND t.date BETWEEN :start AND :end " +
           "GROUP BY DAY(t.date) ORDER BY DAY(t.date)")
    List<DailyTotal> sumByDay(@Param("userId") Long userId,
                              @Param("type") TransactionType type,
                              @Param("start") LocalDate start,
                              @Param("end") LocalDate end);

    List<Transaction> findTop7ByUserIdOrderByDateDescCreatedAtDesc(Long userId);

    interface CategoryTotal {
        String getName();
        Long getTotal();
    }

    interface MonthlyTotal {
        Integer getY();
        Integer getM();
        TransactionType getType();
        Long getTotal();
    }

    interface DailyTotal {
        Integer getDay();
        Long getTotal();
    }
}
