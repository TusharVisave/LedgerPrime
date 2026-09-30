package com.expensetracker.service;

import com.expensetracker.entity.Category;
import com.expensetracker.entity.User;
import com.expensetracker.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Seeds the same default categories the original app created on registration. */
@Service
@RequiredArgsConstructor
public class CategorySeedService {

    private record DefaultCategory(String name, String type, String color) {}

    private static final List<DefaultCategory> DEFAULTS = List.of(
            new DefaultCategory("Food", "expense", "#ef4444"),
            new DefaultCategory("Travel", "expense", "#f97316"),
            new DefaultCategory("Rent", "expense", "#8b5cf6"),
            new DefaultCategory("Bills", "expense", "#0ea5e9"),
            new DefaultCategory("Shopping", "expense", "#14b8a6"),
            new DefaultCategory("Salary", "income", "#16a34a"),
            new DefaultCategory("Freelance", "income", "#84cc16")
    );

    private final CategoryRepository categoryRepository;

    @Transactional
    public void seedFor(User user) {
        for (DefaultCategory def : DEFAULTS) {
            if (categoryRepository.existsByUserIdAndNameAndType(user.getId(), def.name(), def.type())) {
                continue;
            }
            Category category = new Category();
            category.setUser(user);
            category.setName(def.name());
            category.setType(def.type());
            category.setColor(def.color());
            category.setIcon("wallet");
            category.setDefault(true);
            categoryRepository.save(category);
        }
    }
}
