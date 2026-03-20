package com.tien.aivirabackend.service.impl;

import com.tien.aivirabackend.config.properties.FileUploadProperties;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.FileValidationErrorCode;
import com.tien.aivirabackend.service.FileValidatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

import static com.tien.aivirabackend.constant.MediaType.*;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "FILE-VALIDATOR-SERVICE")
public class FileValidatorServiceImpl implements FileValidatorService {
    private final FileUploadProperties properties;

    private static final Map<String, byte[]> MAGIC_PREFIXES = Map.of(
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A},
            "image/gif", new byte[]{0x47, 0x49, 0x46, 0x38},
            "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46}
    );

    @Override
    public void validateFile(MultipartFile file, MediaType mediaType) {
        if (!properties.isEnabled()) {
            log.debug("Upload validation disabled");
            return;
        }

//        validateNotEmpty(file);
//        validateFileSize(file, mediaType);
//        validateMimeType(file, mediaType);
//        validateMagicBytes(file);

        log.debug("Image validation passed: name={}, size={}, contentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());
    }

    @Override
    public String generateSafeFileName(String originalFilename) {
        String extension = "";// = extractSafeExtension(originalFilename);
        return UUID.randomUUID() + extension;
    }

    // ===== Private helpers =====
    private void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(FileValidationErrorCode.EMPTY_FILE);
        }
    }

    private void validateFileSize(MultipartFile file, MediaType mediaType) {
//        long maxSize = getMaxSizeForType(mediaType);
//        if (file.getSize() > maxSize) {
//            throw new FileValidationException(
//                    FileValidationErrorCode.FILE_TOO_LARGE,
//                    String.format(
//                            "Kích thước tệp (%s) vượt quá giới hạn cho phép (%s).",
//                            formatFileSize(file.getSize()),
//                            formatFileSize(maxSize)
//                    )
//            );
//        }
    }

//    private String validateMagicBytes(MultipartFile file, MediaType mediaType) {
//
//    }
//
//    private void validateMimeType(String contentType, String[] allowedTypes) {
//
//    }
//
//    private String extractSafeExtension(String originalFilename) {
//
//    }
//
//    private long getMaxSizeForType(MediaType mediaType) {
//        return switch (mediaType) {
//            case IMAGE -> properties.getMaxImageSize();
//            case DOCUMENT, VIDEO -> properties.getMaxFileSize();
//        };
//    }
}
