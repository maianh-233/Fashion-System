package com.fashion.inventoryservice.dto.request.supplier;

import jakarta.validation.constraints.NotBlank;
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
public class CreateSupplierRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private String contactName;

    private String phone;

    private String email;

    private String address;
}
