package com.fashion.authservice.dto.request.user;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class ChangePasswordRequest {
    private String oldPassword;
    private String newPassword;
}
