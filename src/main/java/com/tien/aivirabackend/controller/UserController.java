package com.tien.aivirabackend.controller;

import org.springframework.web.bind.annotation.*;

import com.tien.aivirabackend.service.UserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j(topic = "USER-CONTROLLER")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    //    @GetMapping
    //    ApiResponse<List<UserResponse>> getUsers() {
    //        List<UserResponse> users = userService.getUsers();
    //        return ApiResponse.<List<UserResponse>>builder()
    //                .data(users)
    //                .build();
    //    }
    //
    //    @GetMapping("/userId")
    //    ApiResponse<UserResponse> getUserById(@PathVariable Long userId) {
    //        UserResponse user = userService.getUser(userId);
    //        return ApiResponse.<UserResponse>builder()
    //                .data(user)
    //                .build();
    //    }

    @PostMapping("/lmao")
    public void test() {
        log.info("LMAO");
    }
}
