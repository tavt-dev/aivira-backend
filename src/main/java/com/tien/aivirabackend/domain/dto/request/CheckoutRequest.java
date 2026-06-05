package com.tien.aivirabackend.domain.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.tien.aivirabackend.constant.PaymentMethod;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Schema(description = "Checkout request. Optional couponCode is evaluated after product/category promotions.")
public class CheckoutRequest {
    @Schema(description = "Customer shipping address id.", example = "5")
    @NotNull
    Long addressId;

    @Schema(description = "Cart item ids selected for this checkout.", example = "[101,102]")
    @NotEmpty
    List<Long> cartItemIds;

    @Schema(description = "COD, VNPAY, or MOMO depending on enabled providers.", example = "COD")
    @NotNull
    PaymentMethod paymentMethod;

    @Schema(description = "Optional coupon code. Blank means no coupon.", example = "AIVIRA10")
    @Size(max = 50)
    String couponCode;

    @Schema(example = "Please call before delivery.")
    @Size(max = 500)
    String notes;
}
