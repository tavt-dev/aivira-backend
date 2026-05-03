package com.tien.aivirabackend.service;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;

import com.tien.aivirabackend.domain.dto.RequestMetadata;

@Service
public class RequestMetadataService {
    public RequestMetadata from(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        String clientIp = forwardedFor == null || forwardedFor.isBlank() ? request.getRemoteAddr() : forwardedFor;
        return new RequestMetadata(request.getHeader("User-Agent"), clientIp);
    }
}
