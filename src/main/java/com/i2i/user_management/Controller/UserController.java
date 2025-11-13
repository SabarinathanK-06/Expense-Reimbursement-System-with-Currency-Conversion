package com.i2i.user_management.Controller;

import com.i2i.user_management.Dto.RegisterDto;
import com.i2i.user_management.Dto.ResetPasswordDto;
import com.i2i.user_management.Dto.UserDto;
import com.i2i.user_management.Exception.BadRequestException;
import com.i2i.user_management.Exception.ValidationException;
import com.i2i.user_management.Service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Controller responsible for managing user operations such as creation,
 * registration, retrieval, update, password changes, and deletion.
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    /**
     * Creates a new user in the system.
     *
     * @param userDto The user details to be created.
     * @return ResponseEntity with the created user and HTTP 201 status.
     */
    @PostMapping("/create")
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserDto userDto) {
        if (userDto == null || userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new ValidationException("User details or email cannot be null or blank");
        }
        logger.info("Creating new user: {}", userDto.getEmail());
        UserDto createdUser = userService.saveUser(userDto);
        logger.info("User created successfully: {}", createdUser.getEmail());
        return new ResponseEntity<>(createdUser, HttpStatus.CREATED);
    }

    /**
     * Registers a new user with the default "USER" role.
     *
     * @param registerDto Registration details including name, email, and password.
     * @return ResponseEntity containing the registered user.
     */
    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterDto registerDto) {
        if (registerDto == null || registerDto.getEmail() == null || registerDto.getEmail().isBlank()) {
            throw new ValidationException("Registration details or email cannot be null or blank");
        }
        logger.info("Registering new user: {}", registerDto.getEmail());
        UserDto registeredUser = userService.registerUser(registerDto);
        logger.info("User registered successfully: {}", registerDto.getEmail());
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }

    /**
     * Retrieves all active users.
     *
     * @return List of UserDto representing all active users.
     */
    @GetMapping("/all")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        logger.info("Fetching all active users");
        List<UserDto> users = userService.findAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * Retrieves a specific user by ID.
     *
     * @param id The UUID of the user.
     * @return User details as ResponseEntity.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID id) {
        if (id == null) {
            throw new ValidationException("User ID cannot be null");
        }
        logger.info("Fetching user by ID: {}", id);
        UserDto user = userService.findUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * Retrieves a user by email.
     *
     * @param email The email address of the user.
     * @return User details as ResponseEntity.
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserDto> getUserByEmail(@PathVariable String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email cannot be null or blank");
        }
        logger.info("Fetching user by email: {}", email);
        UserDto user = userService.findUserByEmail(email);
        return ResponseEntity.ok(user);
    }

    /**
     * Updates an existing user's details.
     *
     * @param id      The UUID of the user to update.
     * @param userDto Updated user details.
     * @return Updated UserDto as ResponseEntity.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable UUID id, @Valid @RequestBody UserDto userDto) {
        if (id == null || userDto == null) {
            throw new ValidationException("User ID or user details cannot be null");
        }
        logger.info("Updating user with ID: {}", id);
        UserDto updatedUser = userService.editUser(userDto, id);
        logger.info("User updated successfully: {}", id);
        return ResponseEntity.ok(updatedUser);
    }

    /**
     * Changes a user's password after validating the old password.
     *
     * @param resetPasswordDto DTO containing old and new passwords.
     * @return HTTP 200 OK response.
     */
    @PutMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ResetPasswordDto resetPasswordDto) {
        if (resetPasswordDto == null ||
                resetPasswordDto.getOldPassword() == null || resetPasswordDto.getNewPassword() == null
                || resetPasswordDto.getConfirmPassword() == null) {
            throw new ValidationException("User ID and passwords cannot be null");
        }

        if (!resetPasswordDto.getConfirmPassword().equals(resetPasswordDto.getNewPassword()))
            throw new BadRequestException("Both new password and confirm password must be equal");

        logger.info("Changing password for current user");
        userService.changePasswordForLoggedInUser(resetPasswordDto.getOldPassword(), resetPasswordDto.getNewPassword());
        logger.info("Password changed successfully for current user");
        return ResponseEntity.ok().build();
    }

    /**
     * Resets a user's password without old password (Only for super admin).
     *
     * @param resetPasswordDto DTO containing old and new passwords.
     * @return HTTP 200 OK response.
     */
    @PutMapping("/{id}/reset-password")
    public ResponseEntity<Void> resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordDto resetPasswordDto) {
        if (!resetPasswordDto.getConfirmPassword().equals(resetPasswordDto.getNewPassword()))
            throw new BadRequestException("Both new password and confirm password must be equal");

        userService.resetPasswordByAdmin(id, resetPasswordDto.getNewPassword());
        return ResponseEntity.ok().build();
    }


    /**
     * Soft deletes a user (marks them inactive).
     *
     * @param id The UUID of the user to delete.
     * @return HTTP 204 No Content on successful deletion.
     */
    @DeleteMapping("/{id}/delete")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        if (id == null) {
            throw new ValidationException("User ID cannot be null");
        }
        logger.info("Deleting user with ID: {}", id);
        userService.deleteUserById(id);
        logger.info("User deleted successfully: {}", id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/testing")
    public void testMethod(@Valid @RequestBody RegisterDto registerDto) {
        System.out.println(" " + registerDto);
    }
}
