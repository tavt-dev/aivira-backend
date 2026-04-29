package com.tien.aivirabackend.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.tien.aivirabackend.config.properties.FileUploadProperties;
import com.tien.aivirabackend.constant.MediaType;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.FileValidationErrorCode;

class FileValidatorServiceImplTest {
    private FileUploadProperties properties;
    private FileValidatorServiceImpl fileValidatorService;

    @BeforeEach
    void setUp() {
        properties = new FileUploadProperties();
        fileValidatorService = new FileValidatorServiceImpl(properties);
    }

    @Test
    void validateFile_shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> fileValidatorService.validateFile(file, MediaType.IMAGE))
                .isInstanceOf(AppException.class)
                .satisfies(ex ->
                        assertThat(((AppException) ex).getErrorCode()).isEqualTo(FileValidationErrorCode.EMPTY_FILE));
    }

    @Test
    void validateFile_shouldRejectFileLargerThanAllowedSize() {
        properties.setMaxImageSize(2);
        MockMultipartFile file =
                new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[] {(byte) 0x89, 0x50, 0x4E});

        assertThatThrownBy(() -> fileValidatorService.validateFile(file, MediaType.IMAGE))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(FileValidationErrorCode.FILE_TOO_LARGE));
    }

    @Test
    void validateFile_shouldRejectUnsupportedMimeType() {
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.txt", "text/plain", "hello".getBytes());

        assertThatThrownBy(() -> fileValidatorService.validateFile(file, MediaType.IMAGE))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(FileValidationErrorCode.INVALID_MIME_TYPE));
    }

    @Test
    void validateFile_shouldRejectInvalidMagicBytes() {
        MockMultipartFile file = new MockMultipartFile("avatar", "avatar.png", "image/png", "not-png".getBytes());

        assertThatThrownBy(() -> fileValidatorService.validateFile(file, MediaType.IMAGE))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getErrorCode())
                        .isEqualTo(FileValidationErrorCode.INVALID_FILE_SIGNATURE));
    }

    @Test
    void validateFile_shouldAcceptValidJpegPngGifAndWebpImages() {
        MockMultipartFile jpeg = new MockMultipartFile(
                "avatar", "avatar.jpg", "image/jpeg", new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00});
        MockMultipartFile png = new MockMultipartFile("avatar", "avatar.png", "image/png", new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00
        });
        MockMultipartFile gif =
                new MockMultipartFile("avatar", "avatar.gif", "image/gif", new byte[] {0x47, 0x49, 0x46, 0x38, 0x00});
        MockMultipartFile webp = new MockMultipartFile("avatar", "avatar.webp", "image/webp", new byte[] {
            0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50
        });

        fileValidatorService.validateFile(jpeg, MediaType.IMAGE);
        fileValidatorService.validateFile(png, MediaType.IMAGE);
        fileValidatorService.validateFile(gif, MediaType.IMAGE);
        fileValidatorService.validateFile(webp, MediaType.IMAGE);
    }
}
