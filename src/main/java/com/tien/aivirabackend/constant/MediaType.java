package com.tien.aivirabackend.constant;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum MediaType {
    @JsonProperty("image")
    IMAGE,

    @JsonProperty("video")
    VIDEO,

    @JsonProperty("document")
    DOCUMENT
}
