package com.tien.aivirabackend.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.constant.PaymentMethod;
import com.tien.aivirabackend.constant.PaymentStatus;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentGroup;

@Repository
public interface PaymentGroupRepository extends JpaRepository<PaymentGroup, Long> {
    boolean existsByPaymentCode(String paymentCode);

    Optional<PaymentGroup> findByPaymentCode(String paymentCode);

    Optional<PaymentGroup> findByProviderTxnRef(String providerTxnRef);

    @EntityGraph(attributePaths = {"payments", "payments.order"})
    Optional<PaymentGroup> findDetailedByPaymentCode(String paymentCode);

    Optional<PaymentGroup> findByPaymentCodeAndUserId(String paymentCode, String userId);

    List<PaymentGroup> findByStatusAndMethodNotAndExpiresAtBefore(
            PaymentStatus status, PaymentMethod method, Instant expiresAt);
}
