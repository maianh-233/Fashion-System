package com.fashion.inventoryservice.dto.request.supplier;

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
public class UpdateSupplierRequest {

    private String name;

    private String contactName;

    private String phone;

    private String email;

    private String address;

    private String status;
}