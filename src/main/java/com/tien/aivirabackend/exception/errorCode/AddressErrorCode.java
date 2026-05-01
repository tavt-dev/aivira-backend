package com.tien.aivirabackend.exception.errorCode;

import org.springframework.http.HttpStatus;

import com.tien.aivirabackend.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AddressErrorCode implements ErrorCode {
    ADDRESS_NOT_FOUND("ADDRESS-001", "Address not found", HttpStatus.NOT_FOUND),
    ADDRESS_NOT_OWNER("ADDRESS-002", "Current user does not own this address", HttpStatus.FORBIDDEN),
    ADDRESS_REQUIRED("ADDRESS-003", "Shipping address is required", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
