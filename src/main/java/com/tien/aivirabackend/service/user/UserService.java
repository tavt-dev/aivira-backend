package com.tien.aivirabackend.service.user;

import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.domain.dto.PageResponse;
import com.tien.aivirabackend.domain.dto.request.UpdatePasswordRequest;
import com.tien.aivirabackend.domain.dto.request.UpdateUserRolesRequest;
import com.tien.aivirabackend.domain.dto.request.UserUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.AdminUserResponse;
import com.tien.aivirabackend.domain.dto.response.UserResponse;

public interface UserService {
    UserResponse getMyProfile();

    UserResponse updateMyProfile(UserUpdateRequest request);

    UserResponse updateMyAvatar(MultipartFile avatarFile);

    void changeMyPassword(UpdatePasswordRequest request);

    void requestDeactivateMyAccount();

    PageResponse<AdminUserResponse> getAdminUsers(
            String keyword, PredefinedRole role, Boolean active, Boolean locked, Boolean emailVerified, int page, int size);

    AdminUserResponse getAdminUser(String userId);

    AdminUserResponse lockUser(String userId);

    AdminUserResponse unlockUser(String userId);

    AdminUserResponse updateUserRoles(String userId, UpdateUserRolesRequest request);

    //    void requestEmailChange(ChangeEmailRequest request);
    //    void confirmEmailChange(VerifyEmailChangeRequest request);
    //
    //    void requestPhoneChange(ChangePhoneRequest request);
    //    void confirmPhoneChange(VerifyPhoneChangeRequest request);
}
