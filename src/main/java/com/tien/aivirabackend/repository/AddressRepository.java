package com.tien.aivirabackend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tien.aivirabackend.domain.entity.user.Address;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdAndActiveTrueOrderByDefaultAddressDescUpdatedAtDesc(String userId);

    Optional<Address> findByIdAndUserIdAndActiveTrue(Long id, String userId);

    boolean existsByUserIdAndActiveTrue(String userId);

    @Modifying
    @Query("update Address a set a.defaultAddress = false where a.user.id = :userId and a.active = true")
    void clearDefaultAddresses(@Param("userId") String userId);
}
