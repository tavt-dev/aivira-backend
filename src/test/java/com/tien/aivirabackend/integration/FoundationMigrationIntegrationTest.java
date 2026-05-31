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
        assertThat(roleRepository.count()).isEqualTo(2);
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

        Integer unexpectedRoleCount = jdbcTemplate.queryForObject(
                """
				SELECT COUNT(*)
				FROM roles
				WHERE code NOT IN ('USER', 'ADMIN')
				""", Integer.class);

        assertThat(unexpectedRoleCount).isZero();

        Integer productPhase4ColumnCount = jdbcTemplate.queryForObject(
                """
				SELECT COUNT(*)
				FROM information_schema.columns
				WHERE table_schema = DATABASE()
				AND table_name = 'products'
				AND column_name IN ('status', 'rejection_reason', 'submitted_at', 'approved_by', 'approved_at', 'rejected_by', 'rejected_at')
				""",
                Integer.class);

        assertThat(productPhase4ColumnCount).isEqualTo(7);

        Integer paymentAttemptTableCount = jdbcTemplate.queryForObject(
                """
				SELECT COUNT(*)
				FROM information_schema.tables
				WHERE table_schema = DATABASE()
				AND table_name = 'payment_attempts'
				""",
                Integer.class);

        Integer paymentAttemptColumnCount = jdbcTemplate.queryForObject(
                """
				SELECT COUNT(*)
				FROM information_schema.columns
				WHERE table_schema = DATABASE()
				AND table_name = 'payment_attempts'
				AND column_name IN ('payment_group_id', 'provider', 'method', 'attempt_no', 'provider_txn_ref', 'request_id', 'status', 'amount', 'raw_request', 'raw_response')
				""",
                Integer.class);

        Integer adminPaymentReconcileCount = jdbcTemplate.queryForObject(
                """
				SELECT COUNT(*)
				FROM role_permissions rp
				JOIN roles r ON r.id = rp.role_id
				JOIN permissions p ON p.id = rp.permission_id
				WHERE r.code = 'ADMIN'
				AND p.code = 'PAYMENT_RECONCILE'
				""",
                Integer.class);

        assertThat(paymentAttemptTableCount).isEqualTo(1);
        assertThat(paymentAttemptColumnCount).isEqualTo(10);
        assertThat(adminPaymentReconcileCount).isEqualTo(1);
    }
}
