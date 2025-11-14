package com.i2i.user_management.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class UserUpdateDto {

    private String firstName;

    private String lastName;

    private String address;

    private String department;

    private String project;

}
