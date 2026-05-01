package com.tien.aivirabackend.service;

import com.tien.aivirabackend.domain.dto.request.CartItemRequest;
import com.tien.aivirabackend.domain.dto.request.CartItemUpdateRequest;
import com.tien.aivirabackend.domain.dto.response.CartResponse;

public interface CartService {
    CartResponse getMyCart();

    CartResponse addItem(CartItemRequest request);

    CartResponse updateItem(Long cartItemId, CartItemUpdateRequest request);

    void removeItem(Long cartItemId);

    void clear();
}
