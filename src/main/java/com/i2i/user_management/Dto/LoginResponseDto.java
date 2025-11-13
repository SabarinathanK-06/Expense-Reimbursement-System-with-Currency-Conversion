package com.i2i.user_management.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponseDto {

    private String token;

    private UUID id;

    private String name;

    private String email;

    private Boolean isActive;

}

