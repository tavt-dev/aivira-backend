package com.tien.aivirabackend.service;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

public interface FileValidatorService {
    void validateFile(MultipartFile file, MediaType mediaType);

    String generateSafeFileName(String originalFilename);
}
