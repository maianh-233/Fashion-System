package com.fashion.customerservice.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fashion.customerservice.constant.AddressType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "customer_addresses")
@Getter 
@Setter
public class CustomerAddress {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private String receiverName;
    private String receiverPhone;

    private String province;
    private String district;
    private String ward;

    @Column(nullable = false)
    private String addressLine;

    private String postalCode;

    private Boolean isDefault;

    @Enumerated(EnumType.STRING)
    private AddressType addressType;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
