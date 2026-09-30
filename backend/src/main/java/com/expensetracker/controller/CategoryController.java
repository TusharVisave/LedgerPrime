package com.expensetracker.controller;

import com.expensetracker.dto.CategoryDto;
import com.expensetracker.dto.CategoryRequest;
import com.expensetracker.security.UserPrincipal;
import com.expensetracker.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryDto> list(@AuthenticationPrincipal UserPrincipal principal) {
        return categoryService.list(principal.getUserId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto create(@AuthenticationPrincipal UserPrincipal principal,
                               @Valid @RequestBody CategoryRequest request) {
        return categoryService.create(principal.getUserId(), request);
    }

    @PutMapping("/{id}")
    public CategoryDto update(@AuthenticationPrincipal UserPrincipal principal,
                              @PathVariable Long id,
                              @Valid @RequestBody CategoryRequest request) {
        return categoryService.update(principal.getUserId(), id, request);
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable Long id) {
        categoryService.delete(principal.getUserId(), id);
        return Map.of("message", "Category deleted");
    }
}
