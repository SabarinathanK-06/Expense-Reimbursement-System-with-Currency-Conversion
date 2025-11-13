package com.i2i.user_management.Dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.util.UUID;

@Data
public class LoginDto {

    @NotNull
    private String email;

    @NotNull
    private String password;

}

