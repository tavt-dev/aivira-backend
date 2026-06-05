package com.tien.aivirabackend.repository.projection;

public interface CategoryHighlightProjection {
    Long getCategoryId();

    String getCategoryName();

    String getSlug();

    String getDescription();

    String getImageUrl();

    String getImagePublicId();

    Integer getDisplayOrder();

    Long getBookCount();
}
