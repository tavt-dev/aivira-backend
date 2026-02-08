package com.tien.aivirabackend.config;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.constant.SignInProvider;
import com.tien.aivirabackend.domain.entity.user.Role;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.repository.RoleRepository;
import com.tien.aivirabackend.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Slf4j
public class ApplicationInitConfig {
    final PasswordEncoder passwordEncoder;

    @Value("${app.seed.admin.username}")
    String adminUsername;

    @Value("${app.seed.admin.password}")
    String adminPassword;

    @Value("${app.seed.admin.email}")
    String adminEmail;

    @Bean
    @ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
    ApplicationRunner applicationRunner(UserRepository userRepository, RoleRepository roleRepository) {
        return args -> {
            log.info("[INIT] Seeding default data...");

            if (!StringUtils.hasText(adminUsername)
                    || !StringUtils.hasText(adminPassword)
                    || !StringUtils.hasText(adminEmail)) {
                log.warn("[INIT] Skip admin seeding: missing app.seed.admin.* config");
                return;
            }

            //
            Role userRole = getOrCreateRole(roleRepository, PredefinedRole.USER, "USER ROLE");
            Role adminRole = getOrCreateRole(roleRepository, PredefinedRole.ADMIN, "ADMIN ROLE");

            getOrCreateRole(roleRepository, PredefinedRole.SELLER, "SELLER ROLE");

            boolean adminExists = userRepository.findByUsername(adminUsername).isPresent()
                    || userRepository.findByEmail(adminEmail).isPresent();

            if (adminExists) {
                log.info("[INIT] Admin already exists. Skip.");
                return;
            }

            Set<Role> roles = new HashSet<>();
            roles.add(userRole);
            roles.add(adminRole);

            User admin = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .email(adminEmail)
                    .emailVerified(true)
                    .isActive(true)
                    .roles(roles)
                    .provider(SignInProvider.LOCAL)
                    .isLocked(false)
                    .isDeleted(false)
                    .build();

            userRepository.save(admin);

            log.warn("[INIT] Admin '{}' created. Please change the password immediately.", adminUsername);
            log.info("[INIT] Seeding completed.");
        };
    }

    private Role getOrCreateRole(RoleRepository roleRepository, PredefinedRole code, String description) {
        return roleRepository
                .findByCode(code)
                .orElseGet(() -> roleRepository.save(
                        Role.builder().code(code).description(description).build()));
    }
}
