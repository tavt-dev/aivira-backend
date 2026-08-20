package com.tien.aivirabackend.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.domain.dto.request.UpdateUserRolesRequest;
import com.tien.aivirabackend.exception.GlobalExceptionHandler;
import com.tien.aivirabackend.service.user.UserService;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerContractTest {
    @Mock
    UserService userService;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminUserController(userService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void listAndDetail_shouldRequireManageOrReadAllPermission() throws Exception {
        assertPreAuthorize(AdminUserController.class.getMethod("getAdminUsers", String.class, PredefinedRole.class,
                Boolean.class, Boolean.class, Boolean.class, int.class, int.class), "USER_MANAGE_ALL", "USER_READ_ALL");
        assertPreAuthorize(AdminUserController.class.getMethod("getAdminUser", String.class), "USER_MANAGE_ALL",
                "USER_READ_ALL");
    }

    @Test
    void lockUnlockAndRoles_shouldRequireExpectedPermissions() throws Exception {
        assertPreAuthorize(AdminUserController.class.getMethod("lockUser", String.class), "USER_MANAGE_ALL",
                "USER_LOCK");
        assertPreAuthorize(AdminUserController.class.getMethod("unlockUser", String.class), "USER_MANAGE_ALL",
                "USER_UNLOCK");
        assertPreAuthorize(
                AdminUserController.class.getMethod("updateUserRoles", String.class, UpdateUserRolesRequest.class),
                "USER_MANAGE_ALL", "USER_ASSIGN_ROLE");
    }

    @Test
    void endpoints_shouldDelegateToUserService() throws Exception {
        mockMvc.perform(get("/admin/users").param("keyword", "alice").param("role", "USER").param("active", "true")
                .param("locked", "false").param("emailVerified", "true").param("page", "2").param("size", "10"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/users/user-1")).andExpect(status().isOk());
        mockMvc.perform(put("/admin/users/user-1/lock")).andExpect(status().isOk());
        mockMvc.perform(put("/admin/users/user-1/unlock")).andExpect(status().isOk());
        mockMvc.perform(put("/admin/users/user-1/roles").contentType(MediaType.APPLICATION_JSON)
                .content("{\"roles\":[\"USER\",\"ADMIN\"]}")).andExpect(status().isOk());

        verify(userService).getAdminUsers("alice", PredefinedRole.USER, true, false, true, 2, 10);
        verify(userService).getAdminUser("user-1");
        verify(userService).lockUser("user-1");
        verify(userService).unlockUser("user-1");
        verify(userService).updateUserRoles(eq("user-1"), any(UpdateUserRolesRequest.class));
    }

    @Test
    void updateUserRoles_whenRolesEmpty_shouldReturnValidationError() throws Exception {
        mockMvc.perform(
                put("/admin/users/user-1/roles").contentType(MediaType.APPLICATION_JSON).content("{\"roles\":[]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("E1100")).andExpect(jsonPath("$.data.roles").exists());
    }

    private void assertPreAuthorize(Method method, String... permissions) {
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertThat(preAuthorize).isNotNull();
        for (String permission : permissions) {
            assertThat(preAuthorize.value()).contains(permission);
        }
    }
}
