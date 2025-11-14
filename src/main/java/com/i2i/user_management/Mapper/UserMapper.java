package com.i2i.user_management.Mapper;

import com.i2i.user_management.Dto.UserDto;
import com.i2i.user_management.Model.Role;
import com.i2i.user_management.Model.User;

import java.util.stream.Collectors;

/**
 * Mapper class for converting between User and UserDto.
 * All mapping logic is centralized here to keep services clean.
 */
public class UserMapper {

    private UserMapper() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts a User entity to a UserDto.
     *
     * @param user the User entity
     * @return mapped UserDto
     */
    public static UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());
        dto.setAddress(user.getAddress());
        dto.setDepartment(user.getDepartment());
        dto.setProject(user.getProject());
        dto.setEmployeeId(user.getEmployeeId());

        if (user.getRoles() != null) {
            dto.setRoleIds(
                    user.getRoles()
                        .stream()
                        .map(Role::getId)
                        .collect(Collectors.toList())
            );
        }

        return dto;
    }
}
