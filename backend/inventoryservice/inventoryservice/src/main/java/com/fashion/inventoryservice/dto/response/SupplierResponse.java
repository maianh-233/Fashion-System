package com.fashion.inventoryservice.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierResponse {

    private UUID id;

    private String code;

    private String name;

    private String contactName;

    private String phone;

    private String email;

    private String address;

    private String status;

    private LocalDateTime createdAt;
}
