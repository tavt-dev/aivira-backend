package com.tien.aivirabackend.domain.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SalesPointResponse {
    LocalDate date;
    BigDecimal revenue;
    Long orderCount;
}
