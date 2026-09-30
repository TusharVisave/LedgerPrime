package com.expensetracker;

import com.expensetracker.entity.Transaction;
import com.expensetracker.entity.TransactionType;
import com.expensetracker.entity.User;
import com.expensetracker.repository.TransactionRepository;
import com.expensetracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setName("Repo Tester");
        user.setEmail("repo@test.local");
        user.setPasswordHash("hashed");
        user = userRepository.save(user);

        saveTx(TransactionType.EXPENSE, 125050, "Food", "lunch at cafe",
                List.of("meals"), LocalDate.of(2026, 9, 10));
        saveTx(TransactionType.EXPENSE, 50000, "Travel", "cab ride",
                List.of("office"), LocalDate.of(2026, 9, 12));
        saveTx(TransactionType.INCOME, 5000000, "Salary", "september salary",
                List.of(), LocalDate.of(2026, 9, 1));
        saveTx(TransactionType.EXPENSE, 20000, "Food", "groceries",
                List.of("home"), LocalDate.of(2026, 8, 20));
    }

    private void saveTx(TransactionType type, long paise, String category, String note,
                        List<String> tags, LocalDate date) {
        Transaction tx = new Transaction();
        tx.setUser(user);
        tx.setType(type);
        tx.setAmountPaise(paise);
        tx.setCategory(category);
        tx.setNote(note);
        tx.setTags(tags);
        tx.setDate(date);
        transactionRepository.save(tx);
    }

    private PageRequest firstPage() {
        return PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "date"));
    }

    @Test
    void search_filtersByType() {
        Page<Transaction> page = transactionRepository.search(
                user.getId(), TransactionType.EXPENSE, null, null, null, null, null, null, firstPage());
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).allMatch(t -> t.getType() == TransactionType.EXPENSE);
    }

    @Test
    void search_filtersByCategory() {
        Page<Transaction> page = transactionRepository.search(
                user.getId(), null, "Food", null, null, null, null, null, firstPage());
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void search_keywordMatchesNoteCategoryAndTags() {
        assertThat(transactionRepository.search(user.getId(), null, null, "%cafe%",
                null, null, null, null, firstPage()).getTotalElements()).isEqualTo(1);
        assertThat(transactionRepository.search(user.getId(), null, null, "%salary%",
                null, null, null, null, firstPage()).getTotalElements()).isEqualTo(1);
        assertThat(transactionRepository.search(user.getId(), null, null, "%office%",
                null, null, null, null, firstPage()).getTotalElements()).isEqualTo(1);
    }

    @Test
    void search_escapedKeywordDoesNotActAsWildcard() {
        // A literal "%" must not match every row.
        Page<Transaction> page = transactionRepository.search(
                user.getId(), null, null, "%\\%%", null, null, null, null, firstPage());
        assertThat(page.getTotalElements()).isEqualTo(0);
    }

    @Test
    void search_filtersByAmountRange() {
        Page<Transaction> page = transactionRepository.search(
                user.getId(), null, null, null, 100000L, 200000L, null, null, firstPage());
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getCategory()).isEqualTo("Food");
    }

    @Test
    void search_filtersByDateRange() {
        Page<Transaction> page = transactionRepository.search(
                user.getId(), null, null, null, null, null,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30), firstPage());
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void search_supportsPagination() {
        Page<Transaction> p1 = transactionRepository.search(
                user.getId(), null, null, null, null, null, null, null,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "date")));
        Page<Transaction> p2 = transactionRepository.search(
                user.getId(), null, null, null, null, null, null, null,
                PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "date")));
        assertThat(p1.getTotalElements()).isEqualTo(4);
        assertThat(p1.getTotalPages()).isEqualTo(2);
        assertThat(p1.getContent()).hasSize(2);
        assertThat(p2.getContent()).hasSize(2);
        assertThat(p1.getContent().get(0).getDate())
                .isAfterOrEqualTo(p1.getContent().get(1).getDate());
    }

    @Test
    void search_isScopedToUser() {
        User other = new User();
        other.setName("Other");
        other.setEmail("other@test.local");
        other.setPasswordHash("hashed");
        userRepository.save(other);
        Page<Transaction> page = transactionRepository.search(
                other.getId(), null, null, null, null, null, null, null, firstPage());
        assertThat(page.getTotalElements()).isEqualTo(0);
    }

    @Test
    void sumByUserIdAndTypeAndDateBetween() {
        long septemberExpenses = transactionRepository.sumByUserIdAndTypeAndDateBetween(
                user.getId(), TransactionType.EXPENSE,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));
        assertThat(septemberExpenses).isEqualTo(175050L);
    }
}
