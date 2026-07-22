package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClaimAnonymousHistoryRequest {
    @NotBlank
    @Size(max = 36)
    private String anonymousId;
}
