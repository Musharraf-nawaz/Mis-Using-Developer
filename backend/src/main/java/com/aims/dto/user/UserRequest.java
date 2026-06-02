package com.aims.dto.user;

import com.aims.entity.enums.Role;
import com.aims.entity.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank
    private String fullName;
    @NotBlank @Email
    private String email;
    private String password;
    @NotNull
    private Role role;
    private String department;
    private String phone;
    private String employeeId;
    private UserStatus status;
}
