package com.expensetracker.dto;

import com.expensetracker.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePasswordRequest {

    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(max = 72, message = "New password must be at most 72 characters")
    @StrongPassword
    private String newPassword;
}
