package com.tien.aivirabackend.service.commerce;

import java.util.List;

import com.tien.aivirabackend.domain.dto.request.AddressRequest;
import com.tien.aivirabackend.domain.dto.response.AddressResponse;

public interface AddressService {
    List<AddressResponse> getMyAddresses();

    AddressResponse create(AddressRequest request);

    AddressResponse update(Long addressId, AddressRequest request);

    void delete(Long addressId);

    AddressResponse setDefault(Long addressId);
}
