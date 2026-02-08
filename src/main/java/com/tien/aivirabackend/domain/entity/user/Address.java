package com.tien.aivirabackend.domain.entity.user;

import jakarta.persistence.*;

import com.tien.aivirabackend.domain.entity.BaseEntity;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Address extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, updatable = false, name = "address_id")
    Long id;

    /* ADDRESS INFO */
    @Column(name = "recipient_name", nullable = false)
    String recipientName;

    @Column(name = "phone_number", nullable = false)
    String phoneNumber;

    @Column(name = "address_line", length = 500, nullable = false)
    String addressLine;

    String ward;
    String district;
    String city;

    @Builder.Default
    @Column(name = "is_default")
    Boolean defaultAddress = false;

    /* RELATIONSHIP */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;
}
