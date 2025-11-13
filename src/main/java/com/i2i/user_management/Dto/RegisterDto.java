package com.i2i.user_management.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterDto {

    @NotNull
    @Pattern(
            regexp = "^[a-z0-9.]+@[a-z0-9.-]+\\.[a-z]{2,}$",
            message = "Invalid email format. Only letters (a-z), numbers (0-9), and periods (.) are allowed. "
                    + "Eg: samuel@gmail.com or muja.mil@i2i.org"
    )
    private String email;

    @NotNull
    @Pattern(
            regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,20}$",
            message = "Password must be 8–20 characters long and contain at least one uppercase letter, "
                    + "one lowercase letter, one digit, and one special character (@#$%^&+=!)."
    )
    private String password;

    @NotNull
    @NotBlank(message = "First Name is required")
    private String firstName;

    @NotNull
    @NotBlank(message = "Last Name is required")
    private String lastName;

    @NotNull
    @NotBlank(message = "Address is required")
    private String address;

    @NotNull
    @NotBlank(message = "Department is required")
    private String department;

    @NotNull
    @NotBlank(message = "Project is required")
    private String project;

}

