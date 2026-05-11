package com.fashion.inventoryservice.dto.request.store;

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
public class UpdateStoreRequest {

    private String name;

    private String address;

    private String phone;

    private Boolean active;
}
