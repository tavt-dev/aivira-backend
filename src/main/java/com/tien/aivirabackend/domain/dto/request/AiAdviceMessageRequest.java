package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AiAdviceMessageRequest(@NotBlank @Pattern(regexp = "^[0-9a-fA-F-]{36}$") String clientMessageId,
        @NotBlank @Size(max = 2000) String content) {
}
