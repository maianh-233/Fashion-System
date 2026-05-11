package com.fashion.inventoryservice.dto.request.store;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStoreRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String name;

    private String address;

    private String phone;
}