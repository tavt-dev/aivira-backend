package com.tien.aivirabackend.service;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryStorageService {
    CloudinaryUploadResult uploadImage(MultipartFile file, String folder, String publicIdPrefix, int width, int height);
}
