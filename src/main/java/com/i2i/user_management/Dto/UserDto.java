package com.i2i.user_management.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UserDto {

    private UUID id;

    @NotNull
    @Pattern(
            regexp = "^[a-z0-9.]+@[a-z0-9.-]+\\.[a-z]{2,}$",
            message = "Invalid email format. Only letters (a-z), numbers (0-9), and periods (.) are allowed."
    )
    private String email;

    @NotNull
    private List<UUID> roleIds;

    @NotNull
    private String firstName;

    @NotNull
    private String lastName;

    private String address;

    @NotNull
    private String department;

    @NotNull
    private String project;

    private String employeeId;

}
