package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.Pattern;

public record AiAdviceSessionCreateRequest(
        @Pattern(regexp = "^(vi|en)(-[A-Za-z]{2})?$", message = "locale must be vi or en") String locale,
        Boolean personalizationEnabled) {}
