package com.i2i.user_management.Service.Impl;

import com.i2i.user_management.Constants.UMSConstants;
import com.i2i.user_management.Dto.RegisterDto;
import com.i2i.user_management.Dto.UserDto;
import com.i2i.user_management.Exception.*;
import com.i2i.user_management.Helper.SecurityContextHelper;
import com.i2i.user_management.Model.Role;
import com.i2i.user_management.Model.User;
import com.i2i.user_management.Repository.RoleRepository;
import com.i2i.user_management.Repository.UserRepository;
import com.i2i.user_management.Service.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service implementation for managing users, handling authentication, registration, and role assignments.
 * Integrates with Spring Security and JWT for secure user operations.
 *
 * @author Sabarinathan
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;

    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;

    @Lazy
    private final AuthenticationManager authenticationManager;

    /**
     * Creates and saves a new user based on the provided {@link UserDto}.
     *
     * <p>Performs validation for duplicate email, assigns roles, and encodes the password before saving.
     *
     * @param userDto user details including name, email, password, and roles
     * @return the saved user in DTO form
     * @throws ValidationException if input details are invalid
     * @throws RoleAssignmentException if any of the assigned roles are deleted or invalid
     */
    @Override
    public UserDto saveUser(UserDto userDto) {
        if (userDto == null) {
            throw new ValidationException("User details cannot be null");
        }
        if (userDto.getEmail() == null || userDto.getEmail().isBlank()) {
            throw new ValidationException("Email cannot be blank");
        }

        logger.info("Saving new user: {}", userDto.getEmail());

        String employeeId = generateEmployeeCode();
        User user = User.builder()
                .email(userDto.getEmail())
                .firstName(userDto.getFirstName())
                .lastName(userDto.getLastName())
                .password(passwordEncoder.encode(employeeId))
                .project(userDto.getProject())
                .department(userDto.getDepartment())
                .employeeId(employeeId)
                .isActive(true)
                .build();

        //assign roles if provided
        if (userDto.getRoleIds() != null && !userDto.getRoleIds().isEmpty()) {
            List<Role> roles = roleRepository.findAllById(userDto.getRoleIds());
            roles.forEach(role -> {
                if (role.getIsDeleted()) {
                    logger.warn("Attempt to assign deleted role: {}", role.getId());
                    throw new RoleAssignmentException("Cannot assign deleted role: " + role.getId());
                }
            });
            user.setRoles(roles.stream().collect(Collectors.toSet()));
        }

        User savedUser = userRepository.save(user);
        logger.info("User saved successfully: {}", savedUser.getEmail());
        return convertToDto(savedUser);
    }

    /**
     * Fetches all active (non-deleted) users.
     *
     * @return a list of active users represented as {@link UserDto}
     */
    @Override
    public List<UserDto> findAllUsers() {
        logger.info("Fetching all active users");
        return userRepository.findAllActiveUsers()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves an active user by their email address.
     *
     * @param email the user's email
     * @return user details in {@link UserDto}
     * @throws ValidationException if email is null or blank
     * @throws UserNotFoundException if user with the given email does not exist
     */
    @Override
    public UserDto findUserByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email cannot be null or blank");
        }

        logger.info("Fetching user by email: {}", email);
        User user = userRepository.findActiveByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        return convertToDto(user);
    }

    /**
     * Retrieves an active user by their unique ID.
     *
     * @param userId unique user identifier
     * @return user details in {@link UserDto}
     * @throws ValidationException if userId is null
     * @throws UserNotFoundException if no active user found for the given ID
     */
    @Override
    public UserDto findUserById(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User ID cannot be null");
        }

        logger.info("Fetching user by ID: {}", userId);
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        return convertToDto(user);
    }

    /**
     * Updates an existing user’s details including name, password, and roles.
     *
     * @param updatedUserDto updated user data
     * @param userId unique identifier of the user to update
     * @return updated {@link UserDto}
     * @throws ValidationException if input data or ID is invalid
     * @throws UserNotFoundException if no user found with the given ID
     * @throws RoleAssignmentException if invalid or deleted roles are assigned
     */
    @Override
    public UserDto editUser(UserDto updatedUserDto, UUID userId) {
        if (userId == null || updatedUserDto == null) {
            throw new ValidationException("User ID or data cannot be null");
        }

        logger.info("Editing user with ID: {}", userId);
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        user.setFirstName(user.getFirstName());
        user.setLastName(user.getLastName());

        if (updatedUserDto.getRoleIds() != null && !updatedUserDto.getRoleIds().isEmpty()) {
            List<Role> roles = roleRepository.findAllById(updatedUserDto.getRoleIds());
            roles.forEach(role -> {
                if (role.getIsDeleted() != null) {
                    logger.warn("Attempt to assign deleted role while updating user: {}", role.getId());
                    throw new RoleAssignmentException("Cannot assign deleted role: " + role.getId());
                }
            });
            user.setRoles(roles.stream().collect(Collectors.toSet()));
        }

        User savedUser = userRepository.save(user);
        logger.info("User updated successfully: {}", userId);
        return convertToDto(savedUser);
    }

    /**
     * Soft deletes a user (marks as deleted) based on their ID.
     * This does not permanently remove the record.
     *
     * @param userId unique user identifier
     * @throws ValidationException if ID is null
     * @throws UserNotFoundException if user not found or already deleted
     */
    @Override
    @Transactional
    public void deleteUserById(UUID userId) {
        if (userId == null) {
            throw new ValidationException("User ID cannot be null");
        }

        logger.info("Deleting user with ID: {}", userId);
        userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        userRepository.softDelete(userId, LocalDateTime.now());
        logger.info("User soft-deleted successfully: {}", userId);
    }

    /**
     * Allows a logged-in user to change their password.
     * Validates the old password and encodes the new one.
     *
     * @param oldPassword user's current password
     * @param newPassword new password to set
     * @throws ValidationException if input is invalid or old password is incorrect
     * @throws UserNotFoundException if logged-in user cannot be found
     */
    @Override
    @Transactional
    public void changePasswordForLoggedInUser(String oldPassword, String newPassword) {
        String currentUserEmail = SecurityContextHelper.extractEmailFromContext();

        if (oldPassword == null || newPassword == null) {
            throw new ValidationException("Passwords cannot be null");
        }

        logger.info("Changing password for user email: {}", currentUserEmail);

        User user = userRepository.findActiveByEmail(currentUserEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new ValidationException("Old password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        logger.info("Password changed successfully for user: {}", currentUserEmail);
    }

    /**
     * Registers a new user in the system with the default 'USER' role.
     * Performs duplicate email validation and password encoding.
     *
     * @param registerDto user registration details
     * @return created user as UserDto
     * @throws ValidationException if input details are invalid
     * @throws RegistrationException if email already exists
     * @throws RoleAssignmentException if default USER role is not found
     */
    @Override
    @Transactional
    public UserDto registerUser(RegisterDto registerDto) {
        if (registerDto == null || registerDto.getEmail() == null || registerDto.getEmail().isBlank()) {
            throw new ValidationException("Registration details or email cannot be null");
        }

        logger.info("Registering new user: {}", registerDto.getEmail());

        if (userRepository.findActiveByEmail(registerDto.getEmail()).isPresent()) {
            throw new RegistrationException("Email already registered: " + registerDto.getEmail());
        }

        User user = User.builder()
                .firstName(registerDto.getFirstName())
                .lastName(registerDto.getLastName())
                .email(registerDto.getEmail())
                .address(registerDto.getAddress())
                .department(registerDto.getDepartment())
                .project(registerDto.getProject())
                .password(passwordEncoder.encode(registerDto.getPassword()))
                .employeeId(generateEmployeeCode())
                .isActive(true)
                .isDeleted(false)
                .build();


        Role userRole = roleRepository.findByName(UMSConstants.EMPLOYEE_ROLE)
                .orElseThrow(() -> new RoleAssignmentException("Default USER role not found. Please seed roles."));

        user.getRoles().add(userRole);

        User savedUser = userRepository.save(user);
        logger.info("User registered successfully: {}", savedUser.getEmail());
        return convertToDto(savedUser);
    }

    /**
     * Loads a user for Spring Security authentication by email.
     *
     * @param email user’s email
     * @return User object implementing org.springframework.security.core.userdetails.UserDetails
     * @throws UsernameNotFoundException if user is not found
     */
    @Override
    public User loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("Email cannot be empty");
        }

        logger.info("Loading user by email: {}", email);
        return userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    /**
     * Allows an admin to reset another user's password.
     *
     * @param userId user ID whose password is to be reset
     * @param newPassword new password to set
     * @throws ValidationException if inputs are invalid
     * @throws UserNotFoundException if user does not exist
     */
    @Override
    @Transactional
    public void resetPasswordByAdmin(UUID userId, String newPassword) {
        if (userId == null || newPassword == null || newPassword.isBlank()) {
            throw new ValidationException("User ID and new password cannot be null or blank");
        }

        logger.info("Admin requested password reset for user ID: {}", userId);

        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        logger.info("Password successfully reset by admin for user ID: {}", userId);
    }

    private String generateEmployeeCode() {
        String year = String.valueOf(Year.now().getValue());

        Optional<String> lastCodeOpt;
        try {
            lastCodeOpt = userRepository.findLastEmployeeCodeForYear(year);
        } catch (Exception e) {
            logger.warn("Failed to find last employee code.");
            throw new DatabaseException("Error retrieving approved expense summaries from database", e);
        }

        int nextNumber = 1;
        if (lastCodeOpt.isPresent()) {
            String lastCode = lastCodeOpt.get();
            String lastFourDigits = lastCode.substring(lastCode.length() - 4);
            nextNumber = Integer.parseInt(lastFourDigits) + 1;
        }

        String formatted = String.format("%04d", nextNumber);
        return "i2i" + year + formatted;
    }

    /**
     * Converts a User entity to a UserDto.
     * Used internally to return clean DTOs to the controller layer.
     *
     * @param user the user entity
     * @return mapped  UserDto
     */
    private UserDto convertToDto(User user) {
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
            dto.setRoleIds(user.getRoles().stream().map(Role::getId).collect(Collectors.toList()));
        }
        return dto;
    }
}
