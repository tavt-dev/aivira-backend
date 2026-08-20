package com.tien.aivirabackend.service.media;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.tien.aivirabackend.exception.ErrorCode;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.ReviewErrorCode;
import com.tien.aivirabackend.exception.errorCode.BlogErrorCode;
import com.tien.aivirabackend.exception.errorCode.UserErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "CLOUDINARY-STORAGE-SERVICE")
public class CloudinaryStorageService {
    private final Cloudinary cloudinary;

    public CloudinaryUploadResult uploadImage(MultipartFile file, String folder, String publicIdPrefix, int width,
            int height) {
        return uploadTransformedImage(file, folder, publicIdPrefix, new Transformation<>().width(width).height(height)
                .crop("fill").gravity("face").quality("auto").fetchFormat("auto"), UserErrorCode.AVATAR_UPLOAD_FAILED);
    }

    public CloudinaryUploadResult uploadReviewImage(MultipartFile file, String folder, String publicIdPrefix,
            int maxWidth, int maxHeight) {
        return uploadTransformedImage(file, folder, publicIdPrefix, new Transformation<>().width(maxWidth)
                .height(maxHeight).crop("limit").quality("auto").fetchFormat("auto"),
                ReviewErrorCode.REVIEW_IMAGE_UPLOAD_FAILED);
    }

    public CloudinaryUploadResult uploadBlogCover(MultipartFile file, String folder, String publicIdPrefix) {
        return uploadTransformedImage(file, folder, publicIdPrefix, new Transformation<>().width(1200).height(630)
                .crop("fill").gravity("auto").quality("auto").fetchFormat("auto"),
                BlogErrorCode.BLOG_IMAGE_UPLOAD_FAILED);
    }

    public CloudinaryUploadResult uploadBlogContentImage(MultipartFile file, String folder, String publicIdPrefix) {
        return uploadTransformedImage(file, folder, publicIdPrefix,
                new Transformation<>().width(1600).height(1600).crop("limit").quality("auto").fetchFormat("auto"),
                BlogErrorCode.BLOG_IMAGE_UPLOAD_FAILED);
    }

    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image", "invalidate", true));
        } catch (IOException | RuntimeException exception) {
            log.warn("Failed to delete Cloudinary image: publicId={}", publicId, exception);
        }
    }

    private CloudinaryUploadResult uploadTransformedImage(MultipartFile file, String folder, String publicIdPrefix,
            Transformation<?> transformation, ErrorCode failureErrorCode) {
        String publicId = buildPublicId(publicIdPrefix);

        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", folder, "public_id", publicId, "resource_type", "image", "overwrite",
                            false, "secure", true, "transformation", transformation));

            String secureUrl = (String) result.get("secure_url");
            String uploadedPublicId = (String) result.get("public_id");

            if (secureUrl == null || uploadedPublicId == null) {
                log.error("Cloudinary upload response missing secure_url/public_id: {}", result);
                throw new AppException(failureErrorCode);
            }

            return new CloudinaryUploadResult(secureUrl, uploadedPublicId);
        } catch (AppException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            log.error("Failed to upload image to Cloudinary: folder={}, publicId={}", folder, publicId, exception);
            throw new AppException(failureErrorCode, exception);
        }
    }

    private String buildPublicId(String publicIdPrefix) {
        String prefix = publicIdPrefix == null || publicIdPrefix.isBlank() ? "image" : publicIdPrefix;
        return prefix + "-" + UUID.randomUUID();
    }
}
