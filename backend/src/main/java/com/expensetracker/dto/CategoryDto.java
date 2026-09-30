package com.expensetracker.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CategoryDto {
    private Long id;
    private String name;
    private String type;
    private String color;
    private String icon;
    private boolean isDefault;
}
