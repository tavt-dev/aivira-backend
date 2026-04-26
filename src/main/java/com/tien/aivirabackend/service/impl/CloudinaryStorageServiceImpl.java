package com.tien.aivirabackend.service.impl;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;
import com.tien.aivirabackend.service.CloudinaryStorageService;
import com.tien.aivirabackend.service.CloudinaryUploadResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "CLOUDINARY-STORAGE-SERVICE")
public class CloudinaryStorageServiceImpl implements CloudinaryStorageService {
    private final Cloudinary cloudinary;

    @Override
    public CloudinaryUploadResult uploadImage(
            MultipartFile file, String folder, String publicIdPrefix, int width, int height) {
        String publicId = buildPublicId(publicIdPrefix);

        try {
            Map<?, ?> result = cloudinary
                    .uploader()
                    .upload(file.getBytes(), ObjectUtils.asMap(
                            "folder", folder,
                            "public_id", publicId,
                            "resource_type", "image",
                            "overwrite", false,
                            "secure", true,
                            "transformation", new Transformation<>()
                                    .width(width)
                                    .height(height)
                                    .crop("fill")
                                    .gravity("face")
                                    .quality("auto")
                                    .fetchFormat("auto")));

            String secureUrl = (String) result.get("secure_url");
            String uploadedPublicId = (String) result.get("public_id");

            if (secureUrl == null || uploadedPublicId == null) {
                log.error("Cloudinary upload response missing secure_url/public_id: {}", result);
                throw new AppException(UserErrorCode.AVATAR_UPLOAD_FAILED);
            }

            return new CloudinaryUploadResult(secureUrl, uploadedPublicId);
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary: folder={}, publicId={}", folder, publicId, e);
            throw new AppException(UserErrorCode.AVATAR_UPLOAD_FAILED, e);
        }
    }

    private String buildPublicId(String publicIdPrefix) {
        String prefix = publicIdPrefix == null || publicIdPrefix.isBlank() ? "image" : publicIdPrefix;
        return prefix + "-" + UUID.randomUUID();
    }
}
