package com.i2i.user_management.Controller;

import com.i2i.user_management.Constants.UMSConstants;
import com.i2i.user_management.Dto.LoginDto;
import com.i2i.user_management.Dto.LoginResponseDto;
import com.i2i.user_management.Integration.Client.ExchangeRateClient;
import com.i2i.user_management.Service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;


/**
 * Controller responsible for managing authentication-related endpoints,
 * including login, logout, and token management.
 *
 * @author Sabarinathan
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    private final ExchangeRateClient exchangeRateClient;

    public AuthController(AuthService authService, ExchangeRateClient exchangeRateClient) {
        this.authService = authService;
        this.exchangeRateClient = exchangeRateClient;
    }

    /**
     * Authenticates a user and generates a JWT token upon successful login.
     * @param loginDto DTO containing the user's email and password.
     * @return {@link org.springframework.http.ResponseEntity} containing
     *         {@link com.i2i.user_management.Dto.LoginResponseDto} with token and user information.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginDto loginDto) {
        LoginResponseDto loginResponse = authService.login(loginDto);
        return ResponseEntity.ok(loginResponse);
    }


    /**
     * Logs out the currently authenticated user by blacklisting their JWT token.
     *
     * @param request HTTP request containing the Authorization header with the JWT token.
     * @return ResponseEntity with success or failure message.
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        String header = request.getHeader(UMSConstants.AUTHORIZATION);

        if (header == null || !header.startsWith(UMSConstants.BEARER_SPACE)) {
            return ResponseEntity.badRequest().body("Missing token");
        }

        String token = header.substring(UMSConstants.INT_SEVEN);
        authService.logout(token);

        return ResponseEntity.ok("Logout successful");
    }


}
