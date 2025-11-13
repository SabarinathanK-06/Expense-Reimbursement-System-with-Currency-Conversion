package com.i2i.user_management.service.impl;

import com.i2i.user_management.Constants.UMSConstants;
import com.i2i.user_management.Dto.UserDto;
import com.i2i.user_management.Exception.RoleAssignmentException;
import com.i2i.user_management.Exception.ValidationException;
import com.i2i.user_management.Model.Role;
import com.i2i.user_management.Model.User;
import com.i2i.user_management.Repository.RoleRepository;
import com.i2i.user_management.Repository.UserRepository;
import com.i2i.user_management.Service.Impl.UserServiceImpl;
import com.i2i.user_management.util.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
