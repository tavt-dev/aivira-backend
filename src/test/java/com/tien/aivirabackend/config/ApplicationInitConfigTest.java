package com.tien.aivirabackend.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.domain.entity.user.Role;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.repository.RoleRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.rbac.PermissionService;
import com.tien.aivirabackend.service.seed.DemoCatalogSeedService;

@ExtendWith(MockitoExtension.class)
class ApplicationInitConfigTest {
    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    UserRepository userRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    PermissionService permissionService;

    @Mock
    DemoCatalogSeedService demoCatalogSeedService;

    ApplicationInitConfig config;

    @BeforeEach
    void setUp() {
        config = new ApplicationInitConfig(passwordEncoder);
    }

    @Test
    void applicationRunner_whenDemoCatalogDisabled_shouldSeedPermissionsAndSkipCatalog() throws Exception {
        setSeedProperties("", "", "", false);

        runner().run(null);

        verify(permissionService).seedDefaultPermissions();
        verify(demoCatalogSeedService, never()).seedDemoCatalog();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void applicationRunner_whenAdminConfigMissingAndDemoEnabled_shouldStillSeedCatalog() throws Exception {
        setSeedProperties("", "", "", true);

        runner().run(null);

        verify(permissionService).seedDefaultPermissions();
        verify(demoCatalogSeedService).seedDemoCatalog();
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void applicationRunner_whenAdminConfigPresent_shouldSeedAdminAndCatalog() throws Exception {
        setSeedProperties("admin", "Password123!", "admin@example.com", true);
        when(roleRepository.findByCode(PredefinedRole.USER)).thenReturn(Optional.empty());
        when(roleRepository.findByCode(PredefinedRole.ADMIN)).thenReturn(Optional.empty());
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123!")).thenReturn("encoded");

        runner().run(null);

        verify(permissionService).seedDefaultPermissions();
        verify(userRepository)
                .save(argThat(user -> user.getUsername().equals("admin") && user.getEmail().equals("admin@example.com")
                        && user.getPassword().equals("encoded") && user.getRoles().size() == 2));
        verify(demoCatalogSeedService).seedDemoCatalog();
    }

    private ApplicationRunner runner() {
        return config.applicationRunner(userRepository, roleRepository, permissionService, demoCatalogSeedService);
    }

    private void setSeedProperties(String username, String password, String email, boolean demoCatalogEnabled) {
        ReflectionTestUtils.setField(config, "adminUsername", username);
        ReflectionTestUtils.setField(config, "adminPassword", password);
        ReflectionTestUtils.setField(config, "adminEmail", email);
        ReflectionTestUtils.setField(config, "demoCatalogEnabled", demoCatalogEnabled);
    }
}
