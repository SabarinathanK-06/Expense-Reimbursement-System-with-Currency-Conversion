package com.i2i.user_management.service.impl;

import com.i2i.user_management.Dto.LoginDto;
import com.i2i.user_management.Model.User;
import com.i2i.user_management.Repository.BlacklistedTokenRepository;
import com.i2i.user_management.Repository.UserRepository;
import com.i2i.user_management.Service.Impl.AuthServiceImpl;
import com.i2i.user_management.Service.Impl.JwtService;
import com.i2i.user_management.util.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BlacklistedTokenRepository blacklistRepo;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private LoginDto loginDto;

    private User user;

    @BeforeEach
    void setUp() {
        loginDto = new LoginDto();
        loginDto.setEmail("user@mail.com");
        loginDto.setPassword("password123");

        user = TestData.getUser();
    }
}
