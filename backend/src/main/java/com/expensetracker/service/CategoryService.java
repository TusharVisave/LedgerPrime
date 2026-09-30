package com.expensetracker.service;

import com.expensetracker.dto.CategoryDto;
import com.expensetracker.dto.CategoryRequest;
import com.expensetracker.entity.Category;
import com.expensetracker.entity.User;
import com.expensetracker.exception.ApiException;
import com.expensetracker.repository.CategoryRepository;
import com.expensetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CategoryDto> list(Long userId) {
        return categoryRepository.findByUserIdOrderByNameAsc(userId).stream()
                .map(this::toDto).toList();
    }

    @Transactional
    public CategoryDto create(Long userId, CategoryRequest request) {
        String name = request.getName().trim();
        if (categoryRepository.existsByUserIdAndNameAndType(userId, name, request.getType())) {
            throw ApiException.conflict("Category already exists");
        }
        Category category = new Category();
        category.setUser(getUser(userId));
        category.setName(name);
        category.setType(request.getType());
        category.setColor(request.getColor() != null ? request.getColor() : "#1d4ed8");
        category.setIcon(request.getIcon() != null ? request.getIcon() : "wallet");
        category.setDefault(false);
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public CategoryDto update(Long userId, Long id, CategoryRequest request) {
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));
        String name = request.getName().trim();
        if (!category.getName().equalsIgnoreCase(name)
                && categoryRepository.existsByUserIdAndNameAndType(userId, name, request.getType())) {
            throw ApiException.conflict("Category already exists");
        }
        category.setName(name);
        if (!category.isDefault()) {
            category.setType(request.getType());
        }
        if (request.getColor() != null) {
            category.setColor(request.getColor());
        }
        if (request.getIcon() != null) {
            category.setIcon(request.getIcon());
        }
        return toDto(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Category category = categoryRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("Category not found"));
        if (category.isDefault()) {
            throw ApiException.badRequest("Default categories cannot be deleted");
        }
        categoryRepository.delete(category);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));
    }

    private CategoryDto toDto(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .type(category.getType())
                .color(category.getColor())
                .icon(category.getIcon())
                .isDefault(category.isDefault())
                .build();
    }
}
