package com.i2i.user_management.Service;

import com.i2i.user_management.Dto.RoleDto;

import java.util.List;
import java.util.UUID;

public interface RoleService {

    RoleDto saveRole(RoleDto roleDto);

    List<RoleDto> findAllRoles();

    RoleDto findRoleById(UUID roleId);

    RoleDto findRoleByName(String name);

    RoleDto editRole(RoleDto updatedRoleDto, UUID roleId);

    void deleteRoleById(UUID roleId); // Soft delete

    void assignRolesToUser(UUID userId,List<UUID> roleIds);

}

