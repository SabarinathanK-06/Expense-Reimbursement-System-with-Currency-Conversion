package com.i2i.user_management.Controller;

import com.i2i.user_management.Dto.AssignRolesDto;
import com.i2i.user_management.Dto.RoleDto;
import com.i2i.user_management.Exception.ValidationException;
import com.i2i.user_management.Service.RoleService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller responsible for managing role operations such as creation, update, retrieval, and deletion.
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    private final RoleService roleService;

    /**
     * Creates a new role in the system.
     *
     * @param roleDto Role details to be created.
     * @return ResponseEntity containing the created role with HTTP 201 status.
     */
    @PostMapping("/create")
    public ResponseEntity<RoleDto> createRole(@RequestBody RoleDto roleDto) {
        if (roleDto == null || roleDto.getName() == null || roleDto.getName().isBlank()) {
            throw new ValidationException("Role name cannot be null or blank");
        }
        logger.info("Creating new role: {}", roleDto.getName());
        RoleDto createdRole = roleService.saveRole(roleDto);
        logger.info("Role created successfully: {}", createdRole.getName());
        return new ResponseEntity<>(createdRole, HttpStatus.CREATED);
    }

    /**
     * Retrieves all active roles from the system.
     *
     * @return List of roles wrapped in ResponseEntity.
     */
    @GetMapping("/all")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        logger.info("Fetching all active roles");
        List<RoleDto> roles = roleService.findAllRoles();
        return ResponseEntity.ok(roles);
    }

    /**
     * Fetches a specific role by its unique ID.
     *
     * @param id Unique identifier of the role.
     * @return RoleDto containing role details.
     */
    @GetMapping("/{id}")
    public ResponseEntity<RoleDto> getRoleById(@PathVariable UUID id) {
        if (id == null) {
            throw new ValidationException("Role ID cannot be null");
        }
        logger.info("Fetching role by ID: {}", id);
        RoleDto role = roleService.findRoleById(id);
        return ResponseEntity.ok(role);
    }

    /**
     * Fetches a role by its name.
     *
     * @param name Name of the role to retrieve.
     * @return RoleDto containing role details.
     */
    @GetMapping("/name/{name}")
    public ResponseEntity<RoleDto> getRoleByName(@PathVariable String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Role name cannot be null or blank");
        }
        logger.info("Fetching role by name: {}", name);
        RoleDto role = roleService.findRoleByName(name);
        return ResponseEntity.ok(role);
    }

    /**
     * Updates an existing role based on the provided ID.
     *
     * @param id      Role ID to update.
     * @param roleDto Updated role details.
     * @return Updated RoleDto wrapped in ResponseEntity.
     */
    @PutMapping("/{id}")
    public ResponseEntity<RoleDto> updateRole(@PathVariable UUID id, @RequestBody RoleDto roleDto) {
        if (id == null || roleDto == null) {
            throw new ValidationException("Role ID or role details cannot be null");
        }
        logger.info("Updating role with ID: {}", id);
        RoleDto updatedRole = roleService.editRole(roleDto, id);
        logger.info("Role updated successfully: {}", id);
        return ResponseEntity.ok(updatedRole);
    }

    /**
     * Assigns one or more roles to a specific user.
     *
     * @param userId         ID of the user to assign roles to.
     * @param assignRolesDto DTO containing list of role IDs to assign.
     * @return HTTP 200 OK response if successful.
     */
    @PostMapping("/assign/{userId}")
    public ResponseEntity<Void> assignRolesToUser(@PathVariable UUID userId,
                                                  @RequestBody AssignRolesDto assignRolesDto) {
        if (userId == null || assignRolesDto == null || assignRolesDto.getRoleIds() == null
                || assignRolesDto.getRoleIds().isEmpty()) {
            throw new ValidationException("User ID and role IDs must be provided");
        }
        logger.info("Assigning roles {} to user ID: {}", assignRolesDto.getRoleIds(), userId);
        roleService.assignRolesToUser(userId, assignRolesDto.getRoleIds());
        logger.info("Roles assigned successfully to user: {}", userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Soft deletes a role by its ID.
     *
     * @param id ID of the role to delete.
     * @return HTTP 204 No Content if successful.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
        if (id == null) {
            throw new ValidationException("Role ID cannot be null");
        }
        logger.info("Deleting role with ID: {}", id);
        roleService.deleteRoleById(id);
        logger.info("Role deleted successfully: {}", id);
        return ResponseEntity.noContent().build();
    }

}
