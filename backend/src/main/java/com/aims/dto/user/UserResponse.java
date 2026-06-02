package com.aims.dto.user;

import com.aims.entity.enums.Role;
import com.aims.entity.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long id;
    private String fullName;
    private String email;
    private Role role;
    private String department;
    private String phone;
    private String employeeId;
    private UserStatus status;
    private LocalDateTime createdAt;
}
