package com.i2i.user_management.service.impl;

import com.i2i.user_management.Constants.UMSConstants;
import com.i2i.user_management.Dto.RegisterDto;
import com.i2i.user_management.Dto.UserDto;
import com.i2i.user_management.Exception.DatabaseException;
import com.i2i.user_management.Exception.RegistrationException;
import com.i2i.user_management.Exception.RoleAssignmentException;
import com.i2i.user_management.Exception.UserNotFoundException;
import com.i2i.user_management.Exception.ValidationException;
import com.i2i.user_management.Helper.SecurityContextHelper;
import com.i2i.user_management.Model.Role;
import com.i2i.user_management.Model.User;
import com.i2i.user_management.Repository.RoleRepository;
import com.i2i.user_management.Repository.UserRepository;
import com.i2i.user_management.Service.Impl.UserServiceImpl;
import com.i2i.user_management.util.TestConstants;
import com.i2i.user_management.util.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    private Role role;

    private UserDto userDto;

    @BeforeEach
    void setUp() {
        user = TestData.getUser();
        role = TestData.getRole(UMSConstants.EMPLOYEE_ROLE);
        userDto = TestData.getUserDto();
    }

    @Test
    void saveUser_Success() {
        //arrange
        userDto.setRoleIds(List.of(UUID.randomUUID()));
        when(userRepository.findLastEmployeeCodeForYear(String.valueOf(Year.now().getValue())))
                .thenReturn(Optional.of("I2I20250007"));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(roleRepository.findAllById(any())).thenReturn(List.of(role));
        when(userRepository.save(any(User.class))).thenReturn(user);

        //act
        UserDto result = userService.saveUser(userDto);

        //assert
        assertNotNull(result);
        assertEquals(user.getEmail(), result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void saveUser_ShouldThrow_WhenRoleDeleted() {
        //arrange
        Role deletedRole = TestData.getRole(UMSConstants.EMPLOYEE_ROLE);
        deletedRole.setIsDeleted(true);
        userDto.setRoleIds(List.of(UUID.randomUUID()));
        when(userRepository.findLastEmployeeCodeForYear(String.valueOf(Year.now().getValue())))
                .thenReturn(Optional.of("I2I20250007"));
        when(roleRepository.findAllById(any())).thenReturn(List.of(deletedRole));

        //act & assert
        assertThrows(RoleAssignmentException.class, () -> userService.saveUser(userDto));
    }

    @Test
    void saveUser_ShouldThrow_WhenEmailBlank() {
        //arrange
        userDto.setEmail("");

        //act & assert
        assertThrows(ValidationException.class, () -> userService.saveUser(userDto));
    }

    @Test
    void findAllUsers_Success() {
        //arrange
        when(userRepository.findAllActiveUsers()).thenReturn(List.of(user));

        //act
        List<UserDto> result = userService.findAllUsers();

        //assert
        assertEquals(1, result.size());
        verify(userRepository, times(1)).findAllActiveUsers();
    }

    @Test
    void findAllUsers_Failure() {
        //arrange
        when(userRepository.findAllActiveUsers()).thenThrow(new RuntimeException("Failed to fetch"));

        //act & assert
        assertThrows(DatabaseException.class, () -> userService.findAllUsers());
    }

    @Test
    void findUserByEmail_Success() {
        //arrange
        when(userRepository.findActiveByEmail(user.getEmail())).thenReturn(Optional.of(user));

        //act
        UserDto result = userService.findUserByEmail(user.getEmail());

        //assert
        assertEquals(user.getEmail(), result.getEmail());
    }

    @Test
    void findUserByEmail_ThrowsUserNotFoundException() {
        //arrange
        when(userRepository.findActiveByEmail(user.getEmail())).thenReturn(Optional.empty());

        //act & assert
        assertThrows(UserNotFoundException.class, () -> userService.findUserByEmail(user.getEmail()));
    }

    @Test
    void findUserByEmail_ThrowsDatabaseException() {
        //arrange
        when(userRepository.findActiveByEmail(user.getEmail())).thenThrow(new RuntimeException("Failed to fetch"));

        //act & assert
        assertThrows(DatabaseException.class, () -> userService.findUserByEmail(user.getEmail()));
    }

    @Test
    void findUserByEmail_ThrowsValidationException() {
        //arrange
        String email = "";

        //act & assert
        assertThrows(ValidationException.class, () -> userService.findUserByEmail(email));
    }

    @Test
    void findUserById_Success() {
        //arrange
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));

        //act
        UserDto result = userService.findUserById(user.getId());

        //assert
        assertEquals(user.getEmail(), result.getEmail());
    }

    @Test
    void findUserById_ShouldThrow_WhenNotFound() {
        //arrange
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.empty());

        //act & assert
        assertThrows(UserNotFoundException.class, () -> userService.findUserById(user.getId()));
    }

    @Test
    void findUserById_ShouldThrow_WhenDatabaseError() {
        //arrange
        when(userRepository.findActiveById(user.getId())).thenThrow(new RuntimeException("Failed to fetch"));

        //act & assert
        assertThrows(DatabaseException.class, () -> userService.findUserById(user.getId()));
    }

    @Test
    void editUser_Success() {
        //arrange
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);

        //act
        UserDto result = userService.editUser(TestData.getPartialUpdateDto(), user.getId());

        //assert
        assertNotNull(result);
        assertEquals("updatedPaari", result.getFirstName());
        assertEquals("seerangan", result.getLastName());

        verify(userRepository).findActiveById(user.getId());
    }

    @Test
    void editUser_UserNotFound_ShouldThrowException() {
        //arrange
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.empty());

        //act & assert
        assertThrows(UserNotFoundException.class,
                () -> userService.editUser(TestData.getPartialUpdateDto(), user.getId()));
    }

    @Test
    void editUser_WhenDatabaseFails_ShouldThrowDatabaseException() {
        //arrange
        when(userRepository.findActiveById(user.getId())).thenThrow(new RuntimeException("Failed to fetch"));

        //act & assert
        assertThrows(DatabaseException.class,
                () -> userService.editUser(TestData.getPartialUpdateDto(), user.getId()));
    }

    @Test
    void editUser_WhenUserIdIsNull_ShouldThrowValidationException() {
        // act & assert
        assertThrows(ValidationException.class, () ->
                userService.editUser(TestData.getUpdateUserDto(), null)
        );
    }

    @Test
    void deleteUserById_Success() {
        //arrange
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));

        //act
        userService.deleteUserById(user.getId());

        //assert
        verify(userRepository, times(1)).softDelete(eq(user.getId()), any(LocalDateTime.class));
    }

    @Test
    void deleteUserById_ShouldThrow_WhenUserNotFound() {
        //arrange
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.empty());

        //act & assert
        assertThrows(UserNotFoundException.class, () -> userService.deleteUserById(user.getId()));
    }

    @Test
    void deleteUserById_ShouldThrow_WhenDatabaseError() {
        //arrange
        when(userRepository.findActiveById(user.getId())).thenThrow(new RuntimeException("Failed to fetch"));

        //act & assert
        assertThrows(DatabaseException.class, () -> userService.deleteUserById(user.getId()));
    }

    @Test
    void registerUser_Success() {
        //arange
        RegisterDto registerDto = TestData.getRegisterDto();
        when(userRepository.findActiveByEmail(registerDto.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(roleRepository.findByName(UMSConstants.EMPLOYEE_ROLE)).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(user);

        //act
        UserDto result = userService.registerUser(registerDto);

        //assert
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void registerUser_ShouldThrow_WhenEmailAlreadyExists() {
        //arrange
        RegisterDto registerDto = TestData.getRegisterDto();
        when(userRepository.findActiveByEmail(registerDto.getEmail())).thenReturn(Optional.of(user));

        //act & assert
        assertThrows(RegistrationException.class, () -> userService.registerUser(registerDto));
    }

    @Test
    void registerUser_ShouldThrow_WhenRoleMissing() {
        //arrange
        RegisterDto registerDto = TestData.getRegisterDto();
        when(userRepository.findActiveByEmail(registerDto.getEmail())).thenReturn(Optional.empty());
        when(roleRepository.findByName(UMSConstants.EMPLOYEE_ROLE)).thenReturn(Optional.empty());

        //act & assert
        assertThrows(RoleAssignmentException.class, () -> userService.registerUser(registerDto));
    }

    @Test
    void loadUserByUsername_Success() {
        //arrange
        when(userRepository.findByEmailWithRoles(user.getEmail())).thenReturn(Optional.of(user));

        //act
        User result = userService.loadUserByUsername(user.getEmail());

        //assert
        assertEquals(user.getEmail(), result.getEmail());
    }

    @Test
    void loadUserByUsername_ShouldThrow_WhenNotFound() {
        //arrange
        when(userRepository.findByEmailWithRoles(user.getEmail())).thenReturn(Optional.empty());

        //act & assert
        assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername(user.getEmail()));
    }

    @Test
    void loadUserByUsername_ShouldThrow_WhenDatabaseError() {
        //arrange
        when(userRepository.findByEmailWithRoles(user.getEmail())).thenThrow(new RuntimeException("Failed to fetch"));

        //act & assert
        assertThrows(DatabaseException.class, () -> userService.loadUserByUsername(user.getEmail()));
    }

    @Test
    void resetPasswordByAdmin_Success() {
        //arrange
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        //act
        userService.resetPasswordByAdmin(user.getId(), "newPass");

        //assert
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void resetPasswordByAdmin_ShouldThrow_WhenNotFound() {
        //arrange
        when(userRepository.findActiveById(user.getId())).thenReturn(Optional.empty());

        //act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.resetPasswordByAdmin(user.getId(), "newPass"));
    }

    @Test
    void resetPasswordByAdmin_ShouldThrow_WhenValidationOccurs() {
        //act & Assert
        assertThrows(ValidationException.class, () -> userService.resetPasswordByAdmin(user.getId(), ""));
    }

    @Test
    void resetPasswordByAdmin_ShouldThrow_WhenDatabaseErrorOccurs() {
        //arrange
        when(userRepository.findActiveById(user.getId())).thenThrow(new RuntimeException("Failed to fetch"));

        //act & Assert
        assertThrows(DatabaseException.class, () -> userService.resetPasswordByAdmin(user.getId(), "newPass"));
    }

    @Test
    void changePassword_Success() {

        try (MockedStatic<SecurityContextHelper> mocked = Mockito.mockStatic(SecurityContextHelper.class)) {
            //arrange
            mocked.when(SecurityContextHelper::extractEmailFromContext).thenReturn(TestConstants.EMAIL);
            when(userRepository.findActiveByEmail(TestConstants.EMAIL)).thenReturn(Optional.of(user));
            when(passwordEncoder.matches("password123", "password123")).thenReturn(true);
            when(passwordEncoder.encode("new123")).thenReturn("encodedNewPassword");
            when(userRepository.save(Mockito.any(User.class))).thenReturn(user);

            //act
            userService.changePasswordForLoggedInUser("password123", "new123");

            //assert
            verify(passwordEncoder).encode("new123");
            verify(userRepository).save(user);
        }
    }

    @Test
    void changePassword_UserNotFound_ShouldThrowException() {

        try (MockedStatic<SecurityContextHelper> mocked = Mockito.mockStatic(SecurityContextHelper.class)) {
            //arrange
            mocked.when(SecurityContextHelper::extractEmailFromContext).thenReturn(TestConstants.EMAIL);
            when(userRepository.findActiveByEmail(TestConstants.EMAIL)).thenReturn(Optional.empty());

            //act & assert
            assertThrows(UserNotFoundException.class, () ->
                    userService.changePasswordForLoggedInUser("old123", "new123")
            );
            verify(passwordEncoder, Mockito.never()).matches(Mockito.any(), Mockito.any());
            verify(userRepository, Mockito.never()).save(Mockito.any());
        }
    }

    @Test
    void changePassword_passwordIsNull_ShouldThrowException() {

        try (MockedStatic<SecurityContextHelper> mocked = Mockito.mockStatic(SecurityContextHelper.class)) {
            //arrange
            mocked.when(SecurityContextHelper::extractEmailFromContext).thenReturn(TestConstants.EMAIL);

            //act & assert
            assertThrows(ValidationException.class, () ->
                    userService.changePasswordForLoggedInUser("old123", null)
            );
        }
    }

}
