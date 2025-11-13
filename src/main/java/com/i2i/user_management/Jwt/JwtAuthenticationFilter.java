package com.i2i.user_management.Jwt;

import com.i2i.user_management.Constants.UMSConstants;
import com.i2i.user_management.Repository.BlacklistedTokenRepository;
import com.i2i.user_management.Service.Impl.JwtService;
import com.i2i.user_management.Service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication filter that validates incoming requests
 * by extracting and verifying JWT tokens from the Authorization header.
 * <p>
 * If the token is valid and not blacklisted, the filter authenticates
 * the user in the Spring Security context.
 * </p>
 *
 * @author Sabarinathan
 */
@Slf4j
@Configuration
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    private final UserService userService;

    private final BlacklistedTokenRepository blacklistRepo;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   UserService userService,
                                   BlacklistedTokenRepository blacklistRepo) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.blacklistRepo = blacklistRepo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String authHeader = request.getHeader(UMSConstants.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith(UMSConstants.BEARER_SPACE)) {
                log.debug("No valid Authorization header found for request: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            String jwt = authHeader.substring(UMSConstants.INT_SEVEN);
            if (jwt.isBlank()) {
                log.warn("Empty JWT token found in request header");
                filterChain.doFilter(request, response);
                return;
            }

            if (blacklistRepo.existsById(jwt)) {
                log.info("Rejected request due to blacklisted token for path: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }

            String username = jwtService.extractUsername(jwt);
            if (username == null || username.isBlank()) {
                log.warn("Invalid JWT token — username could not be extracted");
                filterChain.doFilter(request, response);
                return;
            }

            if (SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt)) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authenticationToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    log.debug("User authenticated successfully: {}", username);
                } else {
                    log.warn("Invalid JWT token for user: {}", username);
                }
            }
        } catch (Exception e) {
            log.error("Error occurred during JWT authentication filter execution: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}
