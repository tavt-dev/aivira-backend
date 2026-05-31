package com.tien.aivirabackend.service.commerce;

import com.tien.aivirabackend.domain.dto.RequestMetadata;
import com.tien.aivirabackend.domain.dto.request.CheckoutRequest;
import com.tien.aivirabackend.domain.dto.response.CheckoutResponse;

public interface CheckoutService {
    CheckoutResponse checkout(CheckoutRequest request, RequestMetadata requestMetadata);
}
