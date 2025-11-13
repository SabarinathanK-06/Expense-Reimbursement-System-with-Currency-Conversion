package com.i2i.user_management.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO used by administrators to reset a user's password.
 * Contains the new password and optional confirmation for validation.
 *
 * @author Sabarinathan
 */
@Getter
@Setter
public class ResetPasswordDto {

    @NotNull
    private String oldPassword;

    @NotNull
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,20}$",
            message = "Password must be 8–20 characters long and contain at least one uppercase letter, "
                    + "one lowercase letter, one digit, and one special character (@#$%^&+=!)."
    )
    private String newPassword;

    @NotNull
    private String confirmPassword;

}
