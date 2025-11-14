package com.i2i.user_management.Service.Impl;

import com.i2i.user_management.Dto.RoleDto;
import com.i2i.user_management.Exception.DatabaseException;
import com.i2i.user_management.Exception.RoleAssignmentException;
import com.i2i.user_management.Exception.RoleNotFoundException;
import com.i2i.user_management.Exception.UserNotFoundException;
import com.i2i.user_management.Exception.ValidationException;
import com.i2i.user_management.Mapper.RoleMapper;
import com.i2i.user_management.Model.Role;
import com.i2i.user_management.Model.User;
import com.i2i.user_management.Repository.RoleRepository;
import com.i2i.user_management.Repository.UserRepository;
import com.i2i.user_management.Service.RoleService;
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

        try {
            Role role = RoleMapper.toEntity(roleDto);

            Role savedRole = roleRepository.save(role);
            logger.info("Role created successfully: {}", savedRole.getName());

            return RoleMapper.toDto(savedRole);
        } catch (Exception e) {
            logger.warn("Database error while saving role '{}': {}",
                    roleDto.getName(), e.getMessage());
            throw new DatabaseException("Failed to save role due to database error");
        }
    }

    /**
     * Retrieves all active roles.
     *
     * @return List of role DTOs.
     */
    @Override
    public List<RoleDto> findAllRoles() {
        logger.info("Fetching all active roles");

        try {
            return roleRepository.findAllActive()
                    .stream()
                    .map(RoleMapper::toDto)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            logger.warn("Database error while fetching all active roles: {}", e.getMessage());
            throw new DatabaseException("Failed to fetch active roles from the database");
        }
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
        try {
            Role role = roleRepository.findActiveById(roleId)
                    .orElseThrow(() -> new RoleNotFoundException("Role not found with id: " + roleId));

            return RoleMapper.toDto(role);
        } catch (RoleNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Database error while fetching role by ID {}: {}",
                    roleId, e.getMessage());
            throw new DatabaseException("Failed to fetch role due to a database error");
        }
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

        try {
            Role role = roleRepository.findActiveByName(name)
                    .orElseThrow(() -> new RoleNotFoundException("Role not found with name: " + name));

            return RoleMapper.toDto(role);
        } catch (RoleNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Database error occurred while fetching role by name '{}': {}",
                    name, e.getMessage());
            throw new DatabaseException("Failed to fetch role due to a database error");
        }
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
        try {
            Role role = roleRepository.findActiveById(roleId)
                    .orElseThrow(() -> new RoleNotFoundException("Role not found"));

            role.setName(updatedRoleDto.getName());
            role.setDescription(updatedRoleDto.getDescription());

            Role savedRole = roleRepository.save(role);
            logger.info("Role updated successfully: {}", roleId);
            return RoleMapper.toDto(savedRole);
        } catch (RoleNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Database error while updating role ID {}: {}",
                    roleId, e.getMessage());
            throw new DatabaseException("Failed to update role due to a database error");
        }
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

        try {
            roleRepository.findActiveById(roleId)
                    .orElseThrow(() -> new RoleNotFoundException("Role not found with ID: " + roleId));
            roleRepository.softDelete(roleId, LocalDateTime.now());
            logger.info("Role soft-deleted successfully: {}", roleId);
        } catch (RoleNotFoundException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Database error while deleting role ID {}: {}",
                    roleId, e.getMessage());
            throw new DatabaseException("Failed to delete role due to a database error");
        }
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

        try {
            User user = userRepository.findActiveById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

            List<Role> roles = roleRepository.findAllById(roleIds);
            if (roles.isEmpty()) {
                throw new RoleAssignmentException("No valid roles found for the provided role IDs");
            }

            Set<Role> activeRoles = roles.stream()
                    .filter(role -> !role.getIsDeleted())
                    .collect(Collectors.toSet());

            if (activeRoles.isEmpty()) {
                throw new RoleAssignmentException("All provided roles are deleted");
            }

            user.setRoles(new HashSet<>(activeRoles));
            userRepository.save(user);
            logger.info("Roles assigned successfully to user: {}", userId);
        } catch (UserNotFoundException | RoleAssignmentException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Database error while assigning roles {} to user {}: {}",
                    roleIds, userId, e.getMessage());
            throw new DatabaseException("Failed to assign roles due to a database error");
        }
    }

}
