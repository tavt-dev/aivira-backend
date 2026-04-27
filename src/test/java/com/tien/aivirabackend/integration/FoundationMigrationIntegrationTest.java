package com.tien.aivirabackend.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.tien.aivirabackend.constant.PermissionCode;
import com.tien.aivirabackend.repository.PermissionRepository;
import com.tien.aivirabackend.repository.RoleRepository;

class FoundationMigrationIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PermissionRepository permissionRepository;

    @Test
    void flywayMigration_shouldCreateSchemaAndSeedRbacReferenceData() {
        assertThat(roleRepository.count()).isEqualTo(3);
        assertThat(permissionRepository.count()).isEqualTo(PermissionCode.values().length);

        Integer adminPermissionCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM role_permissions rp
                JOIN roles r ON r.id = rp.role_id
                WHERE r.code = 'ADMIN'
                """,
                Integer.class);

        Integer userPermissionCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM role_permissions rp
                JOIN roles r ON r.id = rp.role_id
                WHERE r.code = 'USER'
                """,
                Integer.class);

        assertThat(adminPermissionCount).isEqualTo(PermissionCode.values().length);
        assertThat(userPermissionCount).isGreaterThan(0);
    }
}
