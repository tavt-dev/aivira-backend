package com.tien.aivirabackend.integration;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.constant.SignInProvider;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.repository.RoleRepository;
import com.tien.aivirabackend.repository.UserRepository;

import tools.jackson.databind.JsonNode;

class AdminUserIntegrationTest extends AbstractIntegrationTest {
    private static final String PASSWORD = "Password123!";

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Test
    void adminUsers_shouldListFilterAndReturnDetail() throws Exception {
        User admin = saveUser("admin", PredefinedRole.ADMIN);
        User customer = saveUser("alice", PredefinedRole.USER);
        String token = loginToken(admin.getUsername());

        mockMvc.perform(get("/admin/users")
                        .header("Authorization", "Bearer " + token)
                        .param("keyword", "alice")
                        .param("role", "USER")
                        .param("active", "true")
                        .param("locked", "false")
                        .param("emailVerified", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.data[0].id").value(customer.getId()));

        mockMvc.perform(get("/admin/users/{userId}", customer.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("alice"))
                .andExpect(jsonPath("$.data.roles[0].code").value("USER"));
    }

    @Test
    void adminLockAndUnlock_shouldControlLogin() throws Exception {
        User admin = saveUser("admin", PredefinedRole.ADMIN);
        User customer = saveUser("bob", PredefinedRole.USER);
        String adminToken = loginToken(admin.getUsername());

        mockMvc.perform(put("/admin/users/{userId}/lock", customer.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLocked").value(true));

        login(customer.getUsername()).andExpect(status().isForbidden());

        mockMvc.perform(put("/admin/users/{userId}/unlock", customer.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isLocked").value(false));

        login(customer.getUsername()).andExpect(status().isOk());
    }

    @Test
    void adminAssignRole_shouldMakeAdminPermissionsEffectiveAfterNewLogin() throws Exception {
        User admin = saveUser("admin", PredefinedRole.ADMIN);
        User customer = saveUser("charlie", PredefinedRole.USER);
        String adminToken = loginToken(admin.getUsername());

        mockMvc.perform(put("/admin/users/{userId}/roles", customer.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("roles", java.util.List.of("USER", "ADMIN")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles.length()").value(2));

        String customerToken = loginToken(customer.getUsername());
        mockMvc.perform(get("/admin/users").header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminSelfMutations_shouldBeRejected() throws Exception {
        User admin = saveUser("admin", PredefinedRole.ADMIN);
        String token = loginToken(admin.getUsername());

        mockMvc.perform(put("/admin/users/{userId}/lock", admin.getId()).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/admin/users/{userId}/roles", admin.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(json(Map.of("roles", java.util.List.of("USER")))))
                .andExpect(status().isForbidden());
    }

    private User saveUser(String username, PredefinedRole roleCode) {
        var role = roleRepository.findByCode(roleCode).orElseThrow();
        User user = User.builder()
                .username(username)
                .email(username + "@example.com")
                .password(passwordEncoder.encode(PASSWORD))
                .provider(SignInProvider.LOCAL)
                .emailVerified(true)
                .isActive(true)
                .isLocked(false)
                .isDeleted(false)
                .build();
        user.getRoles().add(role);
        return userRepository.save(user);
    }

    private org.springframework.test.web.servlet.ResultActions login(String username) throws Exception {
        return mockMvc.perform(post("/auth/token")
                .contentType(APPLICATION_JSON)
                .content(json(Map.of("username", username, "password", PASSWORD))));
    }

    private String loginToken(String username) throws Exception {
        MvcResult result = login(username).andExpect(status().isOk()).andReturn();
        return read(result, "/data/token").asText();
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private JsonNode read(MvcResult result, String pointer) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString()).at(pointer);
    }
}
