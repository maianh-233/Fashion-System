package com.fashion.inventoryservice.entity;

import java.util.UUID;

import com.fashion.inventoryservice.entity.base.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String name;

    private String address;

    private String phone;

    @Builder.Default
    private Boolean active = true;
}