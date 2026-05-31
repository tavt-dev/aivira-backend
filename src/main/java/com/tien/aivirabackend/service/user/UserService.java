package com.tien.aivirabackend.service.user;

import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.domain.dto.request.UpdatePasswordRequest;
import com.tien.aivirabackend.domain.dto.request.UserUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.UserResponse;

public interface UserService {
    UserResponse getMyProfile();

    UserResponse updateMyProfile(UserUpdateRequest request);

    UserResponse updateMyAvatar(MultipartFile avatarFile);

    void changeMyPassword(UpdatePasswordRequest request);

    void requestDeactivateMyAccount();

    //    void requestEmailChange(ChangeEmailRequest request);
    //    void confirmEmailChange(VerifyEmailChangeRequest request);
    //
    //    void requestPhoneChange(ChangePhoneRequest request);
    //    void confirmPhoneChange(VerifyPhoneChangeRequest request);
}
