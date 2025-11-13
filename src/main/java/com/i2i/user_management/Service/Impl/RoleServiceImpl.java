package com.i2i.user_management.Service.Impl;

import com.i2i.user_management.Dto.RoleDto;
import com.i2i.user_management.Exception.RoleAssignmentException;
import com.i2i.user_management.Exception.RoleNotFoundException;
import com.i2i.user_management.Exception.UserNotFoundException;
import com.i2i.user_management.Exception.ValidationException;
import com.i2i.user_management.Model.Role;
import com.i2i.user_management.Model.User;
import com.i2i.user_management.Repository.RoleRepository;
import com.i2i.user_management.Repository.UserRepository;
import com.i2i.user_management.Service.RoleService;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for managing roles — includes CRUD operations
 * and user-role assignments.
 *
 * @author Sabarinathan
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    private final UserRepository userRepository;

    private static final Logger logger = LoggerFactory.getLogger(RoleServiceImpl.class);

    /**
     * Creates a new role.
     *
     * @param roleDto Role details.
     * @return Created role as DTO.
     */
    @Override
    @Transactional
    public RoleDto saveRole(RoleDto roleDto) {
        if (roleDto == null || roleDto.getName() == null || roleDto.getName().isBlank()) {
            throw new ValidationException("Role name cannot be null or blank");
        }

        logger.info("Creating new role: {}", roleDto.getName());

        Role role = Role.builder()
                .name(roleDto.getName())
                .description(roleDto.getDescription())
                .build();

        Role savedRole = roleRepository.save(role);
        logger.info("Role created successfully: {}", savedRole.getName());
        return convertToDto(savedRole);
    }

    /**
     * Retrieves all active roles.
     *
     * @return List of role DTOs.
     */
    @Override
    public List<RoleDto> findAllRoles() {
        logger.info("Fetching all active roles");
        return roleRepository.findAllActive()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a role by ID.
     *
     * @param roleId Role UUID.
     * @return RoleDto if found.
     */
    @Override
    public RoleDto findRoleById(UUID roleId) {
        if (roleId == null) {
            throw new ValidationException("Role ID cannot be null");
        }

        logger.info("Fetching role by ID: {}", roleId);
        Role role = roleRepository.findActiveById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role not found with id: " + roleId));

        return convertToDto(role);
    }

    /**
     * Retrieves a role by its name.
     *
     * @param name Role name.
     * @return RoleDto if found.
     */
    @Override
    public RoleDto findRoleByName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Role name cannot be null or blank");
        }

        logger.info("Fetching role by name: {}", name);
        Role role = roleRepository.findActiveByName(name)
                .orElseThrow(() -> new RoleNotFoundException("Role not found with name: " + name));

        return convertToDto(role);
    }

    /**
     * Updates an existing role.
     *
     * @param updatedRoleDto Updated role details.
     * @param roleId         Role ID to update.
     * @return Updated RoleDto.
     */
    @Override
    @Transactional
    public RoleDto editRole(RoleDto updatedRoleDto, UUID roleId) {
        if (roleId == null || updatedRoleDto == null) {
            throw new ValidationException("Role ID or details cannot be null");
        }

        logger.info("Updating role with ID: {}", roleId);
        Role role = roleRepository.findActiveById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role not found"));

        role.setName(updatedRoleDto.getName());
        role.setDescription(updatedRoleDto.getDescription());

        Role savedRole = roleRepository.save(role);
        logger.info("Role updated successfully: {}", roleId);
        return convertToDto(savedRole);
    }

    /**
     * Soft deletes a role by marking its deleted timestamp.
     *
     * @param roleId Role ID to delete.
     */
    @Override
    @Transactional
    public void deleteRoleById(UUID roleId) {
        if (roleId == null) {
            throw new ValidationException("Role ID cannot be null");
        }

        logger.info("Deleting role with ID: {}", roleId);
        roleRepository.findActiveById(roleId)
                .orElseThrow(() -> new RoleNotFoundException("Role not found"));
        roleRepository.softDelete(roleId, LocalDateTime.now());
        logger.info("Role soft-deleted successfully: {}", roleId);
    }

    /**
     * Assigns one or more roles to a user.
     *
     * @param userId  User ID.
     * @param roleIds List of role IDs to assign.
     */
    @Override
    @Transactional
    public void assignRolesToUser(UUID userId, List<UUID> roleIds) {
        if (userId == null) {
            throw new ValidationException("User ID cannot be null");
        }
        if (roleIds == null || roleIds.isEmpty()) {
            throw new ValidationException("At least one role ID must be provided");
        }

        logger.info("Assigning roles {} to user: {}", roleIds, userId);

        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        List<Role> roles = roleRepository.findAllById(roleIds);
        if (roles.isEmpty()) {
            throw new RoleAssignmentException("No valid roles found for the provided role IDs");
        }

        Set<Role> activeRoles = roles.stream()
                .filter(role -> role.getIsDeleted() == null)
                .collect(Collectors.toSet());

        if (activeRoles.isEmpty()) {
            throw new RoleAssignmentException("All provided roles are deleted or inactive");
        }

        user.setRoles(new HashSet<>(activeRoles));
        userRepository.save(user);
        logger.info("Roles assigned successfully to user: {}", userId);
    }

    /**
     * Converts a Role entity to RoleDto.
     *
     * @param role Role entity.
     * @return RoleDto with user associations.
     */
    private RoleDto convertToDto(Role role) {
        RoleDto dto = new RoleDto();
        dto.setId(role.getId());
        dto.setName(role.getName());
        dto.setDescription(role.getDescription());
        return dto;
    }
}
