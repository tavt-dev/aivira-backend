package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.NotNull;

public record AiAdvicePreferencesRequest(@NotNull Boolean personalizationEnabled) {}
