package com.tien.aivirabackend.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Login request")
public class AuthenticationRequest {
    @Schema(description = "Username", example = "postman_user")
    String username;

    @Schema(description = "Password", example = "Password123!")
    String password;
}
