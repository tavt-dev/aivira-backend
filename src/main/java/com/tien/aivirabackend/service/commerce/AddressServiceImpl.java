package com.tien.aivirabackend.service.commerce;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.tien.aivirabackend.domain.dto.request.AddressRequest;
import com.tien.aivirabackend.domain.dto.response.AddressResponse;
import com.tien.aivirabackend.domain.entity.user.Address;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.CommerceMapper;
import com.tien.aivirabackend.exception.AppException;
import com.tien.aivirabackend.exception.errorCode.AddressErrorCode;
import com.tien.aivirabackend.repository.AddressRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AddressServiceImpl implements AddressService {
    AddressRepository addressRepository;
    CurrentUserService currentUserService;
    CommerceMapper commerceMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getMyAddresses() {
        User user = currentUserService.getCurrentUser();
        return addressRepository.findByUserIdAndActiveTrueOrderByDefaultAddressDescUpdatedAtDesc(user.getId()).stream()
                .map(commerceMapper::toAddressResponse)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse create(AddressRequest request) {
        User user = currentUserService.getCurrentUser();
        boolean shouldDefault = Boolean.TRUE.equals(request.getDefaultAddress())
                || !addressRepository.existsByUserIdAndActiveTrue(user.getId());
        if (shouldDefault) {
            addressRepository.clearDefaultAddresses(user.getId());
        }
        Address address = Address.builder()
                .user(user)
                .recipientName(request.getRecipientName().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .addressLine(request.getAddressLine().trim())
                .ward(trimToNull(request.getWard()))
                .district(trimToNull(request.getDistrict()))
                .city(trimToNull(request.getCity()))
                .defaultAddress(shouldDefault)
                .active(true)
                .build();
        return commerceMapper.toAddressResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public AddressResponse update(Long addressId, AddressRequest request) {
        User user = currentUserService.getCurrentUser();
        Address address = findMyAddress(addressId, user.getId());
        address.setRecipientName(request.getRecipientName().trim());
        address.setPhoneNumber(request.getPhoneNumber().trim());
        address.setAddressLine(request.getAddressLine().trim());
        address.setWard(trimToNull(request.getWard()));
        address.setDistrict(trimToNull(request.getDistrict()));
        address.setCity(trimToNull(request.getCity()));
        if (Boolean.TRUE.equals(request.getDefaultAddress())) {
            addressRepository.clearDefaultAddresses(user.getId());
            address.setDefaultAddress(true);
        }
        return commerceMapper.toAddressResponse(addressRepository.save(address));
    }

    @Override
    @Transactional
    public void delete(Long addressId) {
        User user = currentUserService.getCurrentUser();
        Address address = findMyAddress(addressId, user.getId());
        boolean wasDefault = Boolean.TRUE.equals(address.getDefaultAddress());
        address.setActive(false);
        address.setDefaultAddress(false);
        addressRepository.save(address);
        if (wasDefault) {
            addressRepository.findByUserIdAndActiveTrueOrderByDefaultAddressDescUpdatedAtDesc(user.getId()).stream()
                    .findFirst()
                    .ifPresent(nextDefault -> {
                        nextDefault.setDefaultAddress(true);
                        addressRepository.save(nextDefault);
                    });
        }
    }

    @Override
    @Transactional
    public AddressResponse setDefault(Long addressId) {
        User user = currentUserService.getCurrentUser();
        Address address = findMyAddress(addressId, user.getId());
        addressRepository.clearDefaultAddresses(user.getId());
        address.setDefaultAddress(true);
        return commerceMapper.toAddressResponse(addressRepository.save(address));
    }

    private Address findMyAddress(Long addressId, String userId) {
        return addressRepository
                .findByIdAndUserIdAndActiveTrue(addressId, userId)
                .orElseThrow(() -> new AppException(AddressErrorCode.ADDRESS_NOT_FOUND));
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
