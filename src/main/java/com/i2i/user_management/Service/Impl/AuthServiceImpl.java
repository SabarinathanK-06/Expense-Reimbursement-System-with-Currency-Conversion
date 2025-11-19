package com.i2i.user_management.Service.Impl;

import com.i2i.user_management.Dto.LoginDto;
import com.i2i.user_management.Dto.LoginResponseDto;
import com.i2i.user_management.Exception.AuthenticationFailedException;
import com.i2i.user_management.Exception.UserNotFoundException;
import com.i2i.user_management.Exception.ValidationException;
import com.i2i.user_management.Model.BlacklistedToken;
import com.i2i.user_management.Model.User;
import com.i2i.user_management.Repository.BlacklistedTokenRepository;
import com.i2i.user_management.Repository.UserRepository;
import com.i2i.user_management.Service.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service implementation responsible for managing authentication-related operations,
 * including login, logout, and token invalidation.
 * <p>
 *
 * @author Sabarinathan
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;

    private final BlacklistedTokenRepository blacklistRepo;

    private final JwtService jwtService;


    /**
     * Authenticates a user using the provided login credentials and generates a JWT token upon success.
     *
     * @param loginDto DTO containing email and password for authentication.
     * @return A LoginResponseDto containing user ID, name, email, status
     *         and generated JWT token.
     * @throws com.i2i.user_management.Exception.ValidationException
     *         if the email or password is missing.
     * @throws com.i2i.user_management.Exception.AuthenticationFailedException
     *         if credentials are invalid or user account is disabled.
     * @throws com.i2i.user_management.Exception.UserNotFoundException
     *         if no active user exists for the given email.
     */
    @Override
    public LoginResponseDto login(LoginDto loginDto) {
        if (loginDto == null || loginDto.getEmail() == null || loginDto.getPassword() == null) {
            throw new ValidationException("Email and password cannot be null");
        }

        logger.info("Attempting login for user: {}", loginDto.getEmail());

        User user = userRepository.findByEmailWithRoles(loginDto.getEmail())
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + loginDto.getEmail()));

        checkAccountLock(user);

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDto.getEmail(),
                            loginDto.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            logger.warn("Invalid login attempt for user: {}", loginDto.getEmail());

            processFailedAttempt(user);
            throw new AuthenticationFailedException("Invalid email or password");
        }

        resetFailedAttempts(user); // On successful login, reset's attempts

        if (!user.isEnabled()) {
            throw new AuthenticationFailedException("User account is disabled");
        }

        String token = jwtService.generateToken(user);

        LoginResponseDto response = new LoginResponseDto();
        response.setToken(token);
        response.setId(user.getId());
        response.setName(user.getFirstName() + user.getLastName());
        response.setEmail(user.getEmail());
        response.setIsActive(user.getIsActive());

        logger.info("User logged in successfully: {}", user.getEmail());
        return response;
    }

    /**
     * Logs out the currently authenticated user by blacklisting their JWT token.
     *
     * @param token JWT token from the request Authorization header.
     * @throws ValidationException           if the token is null or blank.
     * @throws AuthenticationFailedException if the token is invalid or expired.
     */
    @Override
    @Transactional
    public void logout(String token) {
        if (token == null || token.isBlank()) {
            throw new ValidationException("Token cannot be null or blank");
        }

        logger.info("Processing logout request for token: {}", token);

        if (!jwtService.isTokenValid(token)) {
            logger.warn("Attempted to logout with an invalid or expired token");
            throw new AuthenticationFailedException("Invalid or expired token");
        }

        if (blacklistRepo.existsById(token)) {
            logger.info("Token already blacklisted, skipping reinsert");
            return;
        }

        LocalDateTime expiry = jwtService.getTokenExpiry(token);
        blacklistRepo.save(new BlacklistedToken(token, expiry));

        logger.info("Token successfully blacklisted until {}", expiry);
    }

    private void checkAccountLock(User user) {
        LocalDateTime now = LocalDateTime.now();

        if (user.getLockedUntil() != null) {
            if (now.isBefore(user.getLockedUntil())) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                String formattedLockTime = user.getLockedUntil().format(formatter);

                logger.warn("Login blocked. User {} is locked until {}",
                        user.getEmail(), formattedLockTime);
                throw new AuthenticationFailedException("Account is locked until: "
                        + formattedLockTime + ". Try again later.");
            }

            logger.info("Lock expired. User {} is now unlocked.", user.getEmail());
        }
    }

    private void processFailedAttempt(User user) {
        LocalDateTime now = LocalDateTime.now();

        if (user.getLastFailedAttempt() == null ||
                Duration.between(user.getLastFailedAttempt(), now).toHours() >= 1) {
            user.setFailedAttempts(1);
        } else {
            user.setFailedAttempts(user.getFailedAttempts() + 1);
        }
        user.setLastFailedAttempt(now);

        if (user.getFailedAttempts() >= 5) {
            user.setLockedUntil(now.plusDays(1));
            logger.warn("User {} locked until {} due to repeated failed attempts",
                    user.getEmail(), user.getLockedUntil());
        }
        userRepository.save(user);
    }

    private void resetFailedAttempts(User user) {
        if (user.getFailedAttempts() > 0) {
            user.setFailedAttempts(0);
            user.setLastFailedAttempt(null);
            user.setLockedUntil(null);
            userRepository.save(user);
            logger.debug("Failed attempts reset for user {}", user.getEmail());
        }
    }




}
