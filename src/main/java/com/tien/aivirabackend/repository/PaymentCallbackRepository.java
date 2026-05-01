package com.tien.aivirabackend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.constant.PaymentProvider;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentCallback;

@Repository
public interface PaymentCallbackRepository extends JpaRepository<PaymentCallback, Long> {
    Optional<PaymentCallback> findByProviderAndEventKey(PaymentProvider provider, String eventKey);
}
