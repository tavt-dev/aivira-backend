package com.tien.aivirabackend.service;

import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.constant.MediaType;

public interface FileValidatorService {
    void validateFile(MultipartFile file, MediaType mediaType);
}
