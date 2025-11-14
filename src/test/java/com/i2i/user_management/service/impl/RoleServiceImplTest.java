package com.i2i.user_management.service.impl;

import com.i2i.user_management.Constants.UMSConstants;
import com.i2i.user_management.Dto.RoleDto;
import com.i2i.user_management.Exception.DatabaseException;
import com.i2i.user_management.Exception.RoleAssignmentException;
import com.i2i.user_management.Exception.RoleNotFoundException;
import com.i2i.user_management.Exception.UserNotFoundException;
import com.i2i.user_management.Exception.ValidationException;
import com.i2i.user_management.Model.Role;
import com.i2i.user_management.Model.User;
import com.i2i.user_management.Repository.RoleRepository;
import com.i2i.user_management.Repository.UserRepository;
import com.i2i.user_management.Service.Impl.RoleServiceImpl;
import com.i2i.user_management.util.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RoleServiceImplTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    private UUID roleId;

    private UUID userId;

    private Role role;

    private RoleDto roleDto;

    private User user;

    @BeforeEach
    void setUp() {
        roleId = UUID.randomUUID();
        userId = UUID.randomUUID();
        role = TestData.getRole(UMSConstants.EMPLOYEE_ROLE);
        roleDto = new RoleDto();
        roleDto.setName(UMSConstants.EMPLOYEE_ROLE);
        roleDto.setDescription("Employee role");
        user = TestData.getUser();
    }

    @Test
    void saveRole_Success() {
        //arrange
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        //act
        RoleDto result = roleService.saveRole(roleDto);

        //assert
        assertNotNull(result);
        assertEquals(UMSConstants.EMPLOYEE_ROLE, result.getName());
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void saveRole_InvalidData_ShouldThrowValidationException() {
        //act & assert
        assertThrows(ValidationException.class, () -> roleService.saveRole(null));
    }

    @Test
    void saveRole_DatabaseError_ShouldThrowsException() {
        //arrange
        when(roleRepository.save(any(Role.class))).thenThrow(new RuntimeException("Failed to save"));

        //act & assert
        assertThrows(DatabaseException.class, () -> roleService.saveRole(roleDto));
    }

    @Test
    void findAllRoles_Success() {
        //arrange
        when(roleRepository.findAllActive()).thenReturn(List.of(role));

        //act
        List<RoleDto> result = roleService.findAllRoles();

        //assert
        assertEquals(1, result.size());
        assertEquals(UMSConstants.EMPLOYEE_ROLE, result.getFirst().getName());
    }

    @Test
    void findAllRoles_DatabaseError_ShouldThrowsException() {
        //arrange
        when(roleRepository.findAllActive()).thenThrow(new RuntimeException("Failed to fetch"));

        //act & assert
        assertThrows(DatabaseException.class, () -> roleService.findAllRoles());
    }

    @Test
    void findRoleById_Success() {
        //arrange
        when(roleRepository.findActiveById(any())).thenReturn(Optional.of(role));

        //act
        RoleDto result = roleService.findRoleById(roleId);

        //assert
        assertNotNull(result);
        assertEquals(UMSConstants.EMPLOYEE_ROLE, result.getName());
    }

    @Test
    void findRoleById_DatabaseError_ShouldThrowsException() {
        //arrange
        when(roleRepository.findActiveById(any())).thenThrow(new RuntimeException("Failed to fetch"));

        //act & assert
        assertThrows(DatabaseException.class, () -> roleService.findRoleById(roleId));
    }

    @Test
    void findRoleById_Validation_ShouldThrowsException() {
        //act & assert
        assertThrows(ValidationException.class, () -> roleService.findRoleById(null));
    }

    @Test
    void findRoleByName_Success() {
        //arrange
        when(roleRepository.findActiveByName(any())).thenReturn(Optional.of(role));

        //act
        RoleDto result = roleService.findRoleByName(role.getName());

        //assert
        assertNotNull(result);
        assertEquals(UMSConstants.EMPLOYEE_ROLE, result.getName());
    }
    @Test
    void findRoleByName_DatabaseError_ShouldThrowsException() {
        //arrange
        when(roleRepository.findActiveByName(any())).thenThrow(new RuntimeException("Failed to fetch"));

        //act & assert
        assertThrows(DatabaseException.class, () -> roleService.findRoleByName(role.getName()));
    }

    @Test
    void findRoleByName_Validation_ShouldThrowsException() {
        //act & assert
        assertThrows(ValidationException.class, () -> roleService.findRoleByName(null));
    }

    @Test
    void editRole_Success() {
        //arrange
        when(roleRepository.findActiveById(roleId)).thenReturn(Optional.of(role));
        when(roleRepository.save(any(Role.class))).thenReturn(role);

        //act
        RoleDto result = roleService.editRole(roleDto, roleId);

        //assert
        assertNotNull(result);
        assertEquals(UMSConstants.EMPLOYEE_ROLE, result.getName());
        verify(roleRepository).findActiveById(roleId);
        verify(roleRepository).save(role);
    }

    @Test
    void editRole_WhenRoleIdIsNull_ShouldThrowValidationException() {
        //act & assert
        assertThrows(ValidationException.class,
                () -> roleService.editRole(roleDto, null));
    }

    @Test
    void editRole_RoleNotFound_ShouldThrowException() {
        //arrange
        when(roleRepository.findActiveById(roleId)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(RoleNotFoundException.class, () -> roleService.editRole(roleDto, roleId));
        verify(roleRepository).findActiveById(roleId);
        verify(roleRepository, never()).save(any());
    }

    @Test
    void editRole_DatabaseError_ShouldThrowException() {
        //arrange
        when(roleRepository.findActiveById(roleId)).thenThrow(new RuntimeException("Failed to fetch"));

        //act & assert
        assertThrows(DatabaseException.class, () -> roleService.editRole(roleDto, roleId));
    }

    @Test
    void deleteRoleById_Success() {
        //arrange
        when(roleRepository.findActiveById(roleId)).thenReturn(Optional.of(role));

        //act
        roleService.deleteRoleById(roleId);

        //assert
        verify(roleRepository).findActiveById(roleId);
        verify(roleRepository).softDelete(eq(roleId), any(LocalDateTime.class));
    }

    @Test
    void deleteRoleById_NullId_ShouldThrowValidationException() {
        //act & assert
        assertThrows(ValidationException.class,
                () -> roleService.deleteRoleById(null));
        verify(roleRepository, never()).softDelete(any(), any());
    }

    @Test
    void deleteRoleById_RoleNotFound_ShouldThrowException() {
        //arrange
        when(roleRepository.findActiveById(roleId)).thenReturn(Optional.empty());

        //act & assert
        assertThrows(RoleNotFoundException.class,
                () -> roleService.deleteRoleById(roleId));
        verify(roleRepository).findActiveById(roleId);
        verify(roleRepository, never()).softDelete(any(), any());
    }

    @Test
    void deleteRoleById_DbError_ShouldThrowDatabaseException() {
        //arrange
        when(roleRepository.findActiveById(roleId)).thenThrow(new RuntimeException("Failed to fetch"));

        //act & assert
        assertThrows(DatabaseException.class, () -> roleService.deleteRoleById(roleId));
        verify(roleRepository, never()).softDelete(any(), any());
    }

    @Test
    void assignRolesToUser_Success() {
        //arrange
        List<UUID> roleIds = List.of(roleId);
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findAllById(roleIds)).thenReturn(List.of(role));
        when(userRepository.save(any())).thenReturn(user);

        //act
        roleService.assignRolesToUser(userId, roleIds);

        //assert
        assertEquals(1, user.getRoles().size());
        verify(userRepository).save(user);
    }

    @Test
    void assignRolesToUser_NullUserId_ShouldThrowValidationException() {
        //act & assert
        assertThrows(ValidationException.class,
                () -> roleService.assignRolesToUser(null, List.of(roleId)));
    }

    @Test
    void assignRolesToUser_EmptyRoleIds_ShouldThrowValidationException() {
        //arrange
        List<UUID> roleList = new ArrayList<>();

        //act & assert
        assertThrows(ValidationException.class,
                () -> roleService.assignRolesToUser(userId, roleList));
    }

    @Test
    void assignRolesToUser_UserNotFound_ShouldThrowException() {
        //arrange
        List<UUID> roleIds = List.of(roleId);
        when(userRepository.findActiveById(userId)).thenReturn(Optional.empty());

        //act & assert
        assertThrows(UserNotFoundException.class, () -> roleService.assignRolesToUser(userId, roleIds));
        verify(userRepository, never()).save(any());
    }

    @Test
    void assignRolesToUser_NoRolesFound_ShouldThrowException() {
        //arrange
        List<UUID> roleIds = List.of(roleId);
        when(userRepository.findActiveById(userId)).thenReturn(Optional.of(user));
        when(roleRepository.findAllById(roleIds)).thenReturn(List.of());

        //act & assert
        assertThrows(RoleAssignmentException.class, () -> roleService.assignRolesToUser(userId, roleIds));
    }

    @Test
    void assignRolesToUser_DbFailure_ShouldThrowDatabaseException() {
        //arrange
        List<UUID> roleIds = List.of(roleId);
        when(userRepository.findActiveById(userId)).thenThrow(new RuntimeException("Failed to fetch"));

        //act & assert
        assertThrows(DatabaseException.class, () -> roleService.assignRolesToUser(userId, roleIds));
        verify(userRepository, never()).save(any());
    }

}
