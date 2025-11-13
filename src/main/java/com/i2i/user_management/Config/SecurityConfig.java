package com.i2i.user_management.Config;

import com.i2i.user_management.Constants.UMSConstants;
import com.i2i.user_management.Jwt.JwtAuthenticationFilter;
import com.i2i.user_management.Repository.BlacklistedTokenRepository;
import com.i2i.user_management.Service.Impl.JwtService;
import com.i2i.user_management.Service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;


/**
 * SecurityConfig class configures the Spring Security for the application.
 *
 * <p>This class ensures that sensitive APIs are protected using JWT,
 * while allowing public access to login and registration endpoints.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final UserDetailsService userDetailService;

    private final AppBeanConfig appBeanConfig;

    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    public SecurityConfig(@Lazy UserDetailsService userDetailService, AppBeanConfig appBeanConfig) {
        this.userDetailService = userDetailService;
        this.appBeanConfig = appBeanConfig;
    }

    /**
     * Configures the security filter chain for HTTP security.
     * Sets up endpoint access rules, exception handling, CSRF policy,
     * and JWT-based authentication filtering.
     *
     * @param http HttpSecurity object used to configure web security.
     * @param jwtAuthenticationFilter Custom JWT filter for validating tokens.
     * @param authenticationProvider Authentication provider for verifying credentials.
     * @return Configured SecurityFilterChain bean.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   AuthenticationProvider authenticationProvider) throws Exception {
        try {
            http
                    .exceptionHandling(exceptionHandling -> exceptionHandling
                            .authenticationEntryPoint(authenticationEntryPoint())
                            .accessDeniedHandler(accessDeniedHandler())
                    )
                    .csrf(AbstractHttpConfigurer::disable)
                    .authenticationProvider(authenticationProvider)
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/users/register", "/auth/login", "/auth/logout").permitAll()
                            .requestMatchers("/users/all",
                                    "/users/*/reset-password",
                                    "/users/*/delete", "/users/create",
                                    "/users/*/delete", "/users/*",
                                    "/users/email/*", "/roles/create",
                                    "/roles/all", "/roles/*", "/roles/name/*", "/roles/assign/*")
                                    .hasAuthority(UMSConstants.SUPER_ADMIN_ROLE)
                            .requestMatchers("/expenses/*/action", "/expenses/all",
                                    "/expenses/report/approved-per-employee", "/expenses/report/by-currency")
                                    .hasAuthority(UMSConstants.FINANCE_ADMIN_ROLE)
                            .requestMatchers("/users/change-password", "/expenses/create",
                                    "/expenses/*", "/expenses/update", "/expenses/currencies",
                                    "/expenses/*/delete")
                                    .hasAnyAuthority(UMSConstants.EMPLOYEE_ROLE,
                                            UMSConstants.FINANCE_ADMIN_ROLE, UMSConstants.SUPER_ADMIN_ROLE)
                            .anyRequest().authenticated()
                    )
                    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                    .httpBasic(Customizer.withDefaults());
            return http.build();
        } catch (Exception e) {
            logger.error("Error while configuring SecurityFilterChain: {}", e.getMessage(), e);
            throw e;
        }
    }


    /**
     * Defines the entry point for handling unauthorized access (HTTP 401).
     * Triggered when authentication fails or a JWT is missing/invalid.
     *
     * @return HttpStatusEntryPoint that returns 401 Unauthorized.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Custom AccessDeniedHandler that handles forbidden access (HTTP 403).
     * Triggered when a user lacks sufficient privileges to access a resource.
     *
     * @return AccessDeniedHandler that sends 403 Forbidden response.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            logger.warn("Access Denied: {}", accessDeniedException.getMessage());
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.getWriter().write("Access Denied!");
        };
    }

    /**
     * Configures the AuthenticationProvider for Spring Security.
     * Uses a DAO-based provider with UserDetailsService and PasswordEncoder.
     *
     * @return Configured AuthenticationProvider bean.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        try {
            DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
            authProvider.setUserDetailsService(userDetailService);
            authProvider.setPasswordEncoder(appBeanConfig.passwordEncoder());
            logger.debug("AuthenticationProvider initialized successfully.");
            return authProvider;
        } catch (Exception e) {
            logger.error("Failed to initialize AuthenticationProvider: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Provides the AuthenticationManager from AuthenticationConfiguration.
     * Used for authenticating login requests.
     *
     * @param authenticationConfiguration The authentication configuration bean.
     * @return Configured AuthenticationManager.
     * @throws Exception If unable to retrieve authentication manager.
     */

    @Bean
    @Lazy
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        try {
            logger.debug("Loading AuthenticationManager...");
            return authenticationConfiguration.getAuthenticationManager();
        } catch (Exception e) {
            logger.error("Error initializing AuthenticationManager: {}", e.getMessage(), e);
            throw e;
        }

    }

    /**
     * Creates and configures the JWT Authentication Filter.
     * This filter intercepts requests, validates JWTs, and sets user context.
     *
     * @param jwtService The service handling JWT generation and validation.
     * @param userService The user service for loading user details.
     * @param blacklistRepo Repository for checking blacklisted tokens.
     * @return Configured JwtAuthenticationFilter bean.
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtService jwtService, UserService userService, BlacklistedTokenRepository blacklistRepo) {
        try {
            logger.debug("Initializing JwtAuthenticationFilter");
            return new JwtAuthenticationFilter(jwtService, userService, blacklistRepo);
        } catch (Exception e) {
            logger.error("Failed to initialize JwtAuthenticationFilter: {}", e.getMessage(), e);
            throw e;
        }
    }



}

