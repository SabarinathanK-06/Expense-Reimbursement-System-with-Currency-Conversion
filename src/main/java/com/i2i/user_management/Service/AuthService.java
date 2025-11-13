package com.i2i.user_management.Service;

import com.i2i.user_management.Dto.LoginDto;
import com.i2i.user_management.Dto.LoginResponseDto;

public interface AuthService {

    LoginResponseDto login(LoginDto loginDto);

    void logout(String token);


}
