package com.expensetracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CategoryRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 60, message = "Name must be at most 60 characters")
    private String name;

    @NotBlank(message = "Type is required")
    @Pattern(regexp = "^(income|expense)$", message = "Type must be 'income' or 'expense'")
    private String type;

    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Color must be a hex code like #1d4ed8")
    private String color;

    @Size(max = 30, message = "Icon must be at most 30 characters")
    private String icon;
}
