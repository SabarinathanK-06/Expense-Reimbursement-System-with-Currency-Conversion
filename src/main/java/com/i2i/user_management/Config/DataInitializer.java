package com.i2i.user_management.Config;

import com.i2i.user_management.Model.Role;
import com.i2i.user_management.Model.User;
import com.i2i.user_management.Repository.RoleRepository;
import com.i2i.user_management.Repository.UserRepository;
import com.i2i.user_management.util.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * DataInitializer class initializes the default roles (Admin, Manager, User)
 * and ensures a default Admin user is created if not already present.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Value("${admin.role}")
    private String adminRoleName;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("starting data initialization...");

        Optional<User> optionalAdmin = userRepository.findByEmail(ValidationUtils.requestedNonNull(adminEmail));
        boolean adminExists = optionalAdmin.filter(user -> !user.getIsDeleted()).isPresent();

        if (!adminExists) {
            Role adminRole = roleRepository.findByName(adminRoleName)
                    .orElseThrow(() -> new RuntimeException("ADMIN role not found"));

            User admin = User.builder()
                    .firstName("System")
                    .lastName("Admin")
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .employeeId("i2i20250001")
                    .isActive(true)
                    .roles(new HashSet<>(Set.of(adminRole)))
                    .build();


            userRepository.save(admin);
            log.info("created default Admin user: {}", adminEmail);
        } else {
            log.info("Admin user already exists: {}", adminEmail);
        }

        log.info("data initialization completed successfully.");
    }
}
