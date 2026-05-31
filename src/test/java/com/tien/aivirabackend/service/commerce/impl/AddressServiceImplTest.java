package com.tien.aivirabackend.service.commerce.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tien.aivirabackend.domain.dto.request.AddressRequest;
import com.tien.aivirabackend.domain.entity.user.Address;
import com.tien.aivirabackend.domain.entity.user.User;
import com.tien.aivirabackend.domain.mapper.CommerceMapper;
import com.tien.aivirabackend.repository.AddressRepository;
import com.tien.aivirabackend.service.auth.CurrentUserService;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {
    @Mock
    AddressRepository addressRepository;

    @Mock
    CurrentUserService currentUserService;

    CommerceMapper commerceMapper = new CommerceMapper();

    @Test
    void create_shouldMakeFirstAddressDefault() {
        User user = User.builder().id("user-1").build();
        AddressServiceImpl service = new AddressServiceImpl(addressRepository, currentUserService, commerceMapper);
        AddressRequest request = AddressRequest.builder()
                .recipientName("Alice")
                .phoneNumber("0900000000")
                .addressLine("123 Street")
                .defaultAddress(false)
                .build();

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(addressRepository.existsByUserIdAndActiveTrue("user-1")).thenReturn(false);
        when(addressRepository.save(any(Address.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.create(request);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).clearDefaultAddresses("user-1");
        verify(addressRepository).save(captor.capture());
        assertThat(captor.getValue().getDefaultAddress()).isTrue();
        assertThat(captor.getValue().getActive()).isTrue();
    }

    @Test
    void delete_shouldPromoteNextAddressWhenDefaultDeleted() {
        User user = User.builder().id("user-1").build();
        Address current = Address.builder()
                .id(1L)
                .user(user)
                .defaultAddress(true)
                .active(true)
                .build();
        Address next = Address.builder()
                .id(2L)
                .user(user)
                .defaultAddress(false)
                .active(true)
                .build();
        AddressServiceImpl service = new AddressServiceImpl(addressRepository, currentUserService, commerceMapper);

        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(addressRepository.findByIdAndUserIdAndActiveTrue(1L, "user-1")).thenReturn(java.util.Optional.of(current));
        when(addressRepository.findByUserIdAndActiveTrueOrderByDefaultAddressDescUpdatedAtDesc("user-1"))
                .thenReturn(List.of(next));

        service.delete(1L);

        assertThat(current.getActive()).isFalse();
        assertThat(current.getDefaultAddress()).isFalse();
        assertThat(next.getDefaultAddress()).isTrue();
        verify(addressRepository, times(2)).save(any(Address.class));
    }
}
