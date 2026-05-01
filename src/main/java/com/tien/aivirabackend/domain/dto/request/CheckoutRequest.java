package com.tien.aivirabackend.domain.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.tien.aivirabackend.constant.PaymentMethod;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutRequest {
    @NotNull
    Long addressId;

    @NotEmpty
    List<Long> cartItemIds;

    @NotNull
    PaymentMethod paymentMethod;

    @Size(max = 500)
    String notes;
}
