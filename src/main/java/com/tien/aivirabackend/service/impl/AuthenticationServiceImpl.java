package com.tien.aivirabackend.service.impl;

import com.tien.aivirabackend.constant.PredefinedRole;
import com.tien.aivirabackend.constant.SignInProvider;
import com.tien.aivirabackend.domain.dto.request.AuthenticationRequest;
import com.tien.aivirabackend.domain.dto.request.UserRegisterRequest;
import com.tien.aivirabackend.domain.dto.response.AuthenticationResponse;
import com.tien.aivirabackend.domain.dto.response.UserResponse;
import com.tien.aivirabackend.domain.entity.user.Role;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.UserMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AuthErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.repository.RoleRepository;
import com.tien.aivirabackend.repository.UserRepository;
import com.tien.aivirabackend.service.AuthenticationService;
import com.tien.aivirabackend.service.JwtService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;

@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Slf4j(topic = "AUTHENTICATION-SERVICE")
public class AuthenticationServiceImpl implements AuthenticationService {

    PasswordEncoder passwordEncoder;

    UserRepository userRepository;

    RoleRepository roleRepository;

    UserMapper userMapper;

    JwtService jwtService;

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(UserErrorCode.USER_NOT_FOUND));

        boolean isAuthenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if(!isAuthenticated) {
            log.error("Authentication failed for user: {}", request.getUsername());
            throw new AppException(AuthErrorCode.PASSWORD_INCORRECT);
        }

//        if(!user.getIsActive()) {
//            log.error("Authentication failed for user: {}. Reason: Account is inactive.", request.getUsername());
//            throw new AppException(AuthErrorCode.ACCOUNT_DISABLED);
//        }

        String token = jwtService.createAccessToken(user);

        return AuthenticationResponse.builder()
                .token(token)
                .authenticated(true)
                .build();
    }

    @Override
    public UserResponse register(UserRegisterRequest request) {
        //Validate
        if(userRepository.existsByUsername(request.getUsername())) {
            log.error("Đăng kí thất bại. Username: {} đã được sử dụng.", request.getUsername());
            throw new AppException(UserErrorCode.USERNAME_ALREADY_EXISTS);
        }
        if(userRepository.existsByEmail(request.getEmail())) {
            log.error("Đăng kí thất bại. Email: {} đã được sử dụng.", request.getEmail());
            throw new AppException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = userMapper.toUser(request);

        //Set auth
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setProvider(SignInProvider.LOCAL);
        user.setProviderUserId(null);

        //Default status
        user.setEmailVerified(false);
        user.setIsActive(false);
        user.setIsLocked(false);
        user.setIsDeleted(false);

        var role = roleRepository.findByCode(PredefinedRole.USER)
                .orElseThrow(() -> new AppException(UserErrorCode.ROLE_NOT_FOUND));

        user.getRoles().add(role);

        User savedUser = userRepository.save(user);

        return userMapper.toUserResponse(savedUser);
    }
}
