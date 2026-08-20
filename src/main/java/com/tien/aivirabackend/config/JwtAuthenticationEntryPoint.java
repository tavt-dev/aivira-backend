package com.tien.aivirabackend.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.tien.aivirabackend.domain.dto.ApiResponse;
import com.tien.aivirabackend.exception.ErrorCode;
import com.tien.aivirabackend.exception.errorCode.AuthErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        if (response.isCommitted()) {
            return;
        }

        ErrorCode errorCode = AuthErrorCode.AUTHENTICATION_FAILED;

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ApiResponse<?> apiResponse = ApiResponse.error(errorCode.getCode(), errorCode.getMessage());
        log.warn("Unauthorized: method={} path={} ip={} reason={}", request.getMethod(), request.getRequestURI(),
                request.getRemoteAddr(), authException.getMessage());

        objectMapper.writeValue(response.getWriter(), apiResponse);

        response.flushBuffer();
    }
}
