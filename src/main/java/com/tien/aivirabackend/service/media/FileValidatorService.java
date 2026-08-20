package com.tien.aivirabackend.service.media;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.tien.aivirabackend.config.properties.FileUploadProperties;
import com.tien.aivirabackend.constant.MediaType;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.FileValidationErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "FILE-VALIDATOR-SERVICE")
public class FileValidatorService {
    private static final Map<String, byte[]> MAGIC_PREFIXES = Map.of("image/jpeg",
            new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF }, "image/png",
            new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A }, "image/gif",
            new byte[] { 0x47, 0x49, 0x46, 0x38 }, "application/pdf", new byte[] { 0x25, 0x50, 0x44, 0x46 });

    private final FileUploadProperties properties;

    public void validateFile(MultipartFile file, MediaType mediaType) {
        if (!properties.isEnabled()) {
            log.debug("Upload validation disabled");
            return;
        }

        validateNotEmpty(file);
        validateFileSize(file, mediaType);
        validateMimeType(file, mediaType);
        validateMagicBytes(file);

        log.debug("File validation passed: name={}, size={}, contentType={}", file.getOriginalFilename(),
                file.getSize(), file.getContentType());
    }

    private void validateNotEmpty(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(FileValidationErrorCode.EMPTY_FILE);
        }
    }

    private void validateFileSize(MultipartFile file, MediaType mediaType) {
        long maxSize = getMaxSizeForType(mediaType);
        if (file.getSize() > maxSize) {
            throw new AppException(FileValidationErrorCode.FILE_TOO_LARGE).addDetail("maxSize", maxSize)
                    .addDetail("actualSize", file.getSize());
        }
    }

    private void validateMimeType(MultipartFile file, MediaType mediaType) {
        String contentType = normalizeContentType(file.getContentType());
        List<String> allowedMimeTypes = getAllowedMimeTypes(mediaType);

        if (!StringUtils.hasText(contentType) || !allowedMimeTypes.contains(contentType)) {
            throw new AppException(FileValidationErrorCode.INVALID_MIME_TYPE)
                    .addDetail("contentType", file.getContentType()).addDetail("allowedTypes", allowedMimeTypes);
        }
    }

    private void validateMagicBytes(MultipartFile file) {
        String contentType = normalizeContentType(file.getContentType());

        try {
            byte[] header = file.getInputStream().readNBytes(12);
            if ("image/webp".equals(contentType)) {
                validateWebpSignature(header);
                return;
            }

            byte[] expectedPrefix = MAGIC_PREFIXES.get(contentType);
            if (expectedPrefix == null || !startsWith(header, expectedPrefix)) {
                throw new AppException(FileValidationErrorCode.INVALID_FILE_SIGNATURE);
            }
        } catch (IOException e) {
            throw new AppException(FileValidationErrorCode.FILE_READ_ERROR, e);
        }
    }

    private void validateWebpSignature(byte[] header) {
        boolean valid = header.length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F'
                && header[3] == 'F' && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';

        if (!valid) {
            throw new AppException(FileValidationErrorCode.INVALID_FILE_SIGNATURE);
        }
    }

    private boolean startsWith(byte[] actual, byte[] expectedPrefix) {
        if (actual.length < expectedPrefix.length) {
            return false;
        }

        for (int i = 0; i < expectedPrefix.length; i++) {
            if (actual[i] != expectedPrefix[i]) {
                return false;
            }
        }
        return true;
    }

    private List<String> getAllowedMimeTypes(MediaType mediaType) {
        return switch (mediaType) {
        case IMAGE -> properties.getAllowedImageTypes();
        case DOCUMENT -> properties.getAllowedDocumentTypes();
        case VIDEO -> throw new AppException(FileValidationErrorCode.UNSUPPORTED_MEDIA_CATEGORY);
        };
    }

    private long getMaxSizeForType(MediaType mediaType) {
        return switch (mediaType) {
        case IMAGE -> properties.getMaxImageSize();
        case DOCUMENT -> properties.getMaxDocumentSize();
        case VIDEO -> throw new AppException(FileValidationErrorCode.UNSUPPORTED_MEDIA_CATEGORY);
        };
    }

    private String normalizeContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return null;
        }
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }
}
