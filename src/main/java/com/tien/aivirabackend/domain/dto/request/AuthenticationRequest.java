package com.tien.aivirabackend.domain.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
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
    @NotBlank(message = "Username must not be blank")
    String username;

    @Schema(description = "Password", example = "Password123!")
    @NotBlank(message = "Password must not be blank")
    String password;
}
