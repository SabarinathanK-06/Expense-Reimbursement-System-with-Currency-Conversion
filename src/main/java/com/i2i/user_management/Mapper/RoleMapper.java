package com.i2i.user_management.Mapper;

import com.i2i.user_management.Dto.RoleDto;
import com.i2i.user_management.Model.Role;

/**
 * Mapper class responsible for converting Role entities to RoleDto objects
 * and vice versa. This class centralizes all Role-related transformation
 * logic to maintain cleaner service layers.
 */
public class RoleMapper {

    private RoleMapper() {
    }

    /**
     * Converts Role entity to RoleDto.
     *
     * @param role Role entity.
     * @return RoleDto.
     */
    public static RoleDto toDto(Role role) {
        if (role == null) {
            return null;
        }

        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        return dto;
    }

    /**
     * Converts RoleDto to Role entity.
     *
     * @param dto RoleDto.
     * @return Role entity.
     */
    public static Role toEntity(RoleDto dto) {
        if (dto == null) {
            return null;
        }

        return Role.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
    }
}
