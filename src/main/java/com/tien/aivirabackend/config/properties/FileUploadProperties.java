package com.tien.aivirabackend.config.properties;


import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Data
@Validated
@ConfigurationProperties(prefix = "app.upload")
public class FileUploadProperties {
    private boolean enabled = true;

    @Min(1)
    private long maxImageSize = 5 * 1024 * 1024L;

    @Min(1)
    private long maxDocumentSize = 10 * 1024 * 1024L;

    @NotEmpty
    private List<String> allowedImageTypes = List.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    @NotEmpty
    private List<String> allowedDocumentTypes = List.of(
            "application/pdf"
    );
}
