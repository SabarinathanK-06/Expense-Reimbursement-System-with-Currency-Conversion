package com.i2i.user_management.Dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class RoleDto {

    private UUID id;

    private String name;

    private String description;

    private List<UUID> userIds;

}

