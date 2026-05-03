package com.tien.aivirabackend.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.constant.PaymentProvider;
import com.tien.aivirabackend.domain.entity.transaction.payment.PaymentAttempt;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {
    int countByPaymentGroupId(Long paymentGroupId);

    Optional<PaymentAttempt> findTopByPaymentGroupIdOrderByAttemptNoDesc(Long paymentGroupId);

    Optional<PaymentAttempt> findByProviderAndProviderTxnRef(PaymentProvider provider, String providerTxnRef);

    Optional<PaymentAttempt> findByRequestId(String requestId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from PaymentAttempt a join fetch a.paymentGroup g where a.id = :id")
    Optional<PaymentAttempt> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "select a from PaymentAttempt a join fetch a.paymentGroup g where a.provider = :provider and a.providerTxnRef = :providerTxnRef")
    Optional<PaymentAttempt> findByProviderAndProviderTxnRefForUpdate(
            @Param("provider") PaymentProvider provider, @Param("providerTxnRef") String providerTxnRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from PaymentAttempt a join fetch a.paymentGroup g where a.requestId = :requestId")
    Optional<PaymentAttempt> findByRequestIdForUpdate(@Param("requestId") String requestId);
}
