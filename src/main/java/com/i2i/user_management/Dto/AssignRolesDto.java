package com.i2i.user_management.Dto;

import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class AssignRolesDto {

    private List<UUID> roleIds;

}

