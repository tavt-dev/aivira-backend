package com.tien.aivirabackend.domain.dto.request;

import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Admin reply request. Blank reply clears the existing admin reply.")
public class ReviewReplyRequest {
    @Schema(example = "Thank you for the feedback. We will keep improving our book packaging.")
    @Size(max = 2000)
    String adminReply;
}
