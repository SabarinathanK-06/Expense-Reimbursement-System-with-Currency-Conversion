package com.i2i.user_management.Service;

import com.i2i.user_management.Dto.LoginDto;
import com.i2i.user_management.Dto.LoginResponseDto;
import com.i2i.user_management.Dto.RegisterDto;
import com.i2i.user_management.Dto.UserDto;
import com.i2i.user_management.Dto.UserUpdateDto;
import com.i2i.user_management.Model.User;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserDto saveUser(UserDto userDto);

    List<UserDto> findAllUsers();

    UserDto findUserByEmail(String email);

    UserDto findUserById(UUID userId);

    UserDto editUser(UserUpdateDto updatedUserDto, UUID userId);

    void deleteUserById(UUID userId); // Soft delete

    void changePasswordForLoggedInUser(String oldPassword, String newPassword);

    UserDto registerUser(RegisterDto registerDto);

    User loadUserByUsername(String email);

    void resetPasswordByAdmin(UUID userId, String newPassword);


}
