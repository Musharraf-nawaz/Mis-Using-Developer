package com.aims.service;

import com.aims.dto.common.PageResponse;
import com.aims.dto.user.UserRequest;
import com.aims.dto.user.UserResponse;
import com.aims.entity.User;
import com.aims.entity.enums.Role;
import com.aims.entity.enums.UserStatus;
import com.aims.exception.BadRequestException;
import com.aims.exception.ResourceNotFoundException;
import com.aims.repository.UserRepository;
import com.aims.util.MapperUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getAll(String search, Role role, UserStatus status, Pageable pageable) {
        Page<User> page = userRepository.findWithFilters(search, role, status, pageable);
        return PageResponse.from(page.map(MapperUtils::toUserResponse));
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return MapperUtils.toUserResponse(findUser(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already exists");
        }
        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(
                        request.getPassword() != null ? request.getPassword() : "Employee@123"))
                .role(request.getRole())
                .department(request.getDepartment())
                .phone(request.getPhone())
                .employeeId(request.getEmployeeId())
                .status(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE)
                .build();
        user = userRepository.save(user);
        auditService.log("USER_CREATED", "USER", user.getId(), null, user.getEmail());
        return MapperUtils.toUserResponse(user);
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        User user = findUser(id);
        user.setFullName(request.getFullName());
        user.setDepartment(request.getDepartment());
        user.setPhone(request.getPhone());
        user.setEmployeeId(request.getEmployeeId());
        user.setRole(request.getRole());
        if (request.getStatus() != null) user.setStatus(request.getStatus());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user = userRepository.save(user);
        auditService.log("USER_UPDATED", "USER", user.getId(), null, user.getEmail());
        return MapperUtils.toUserResponse(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = findUser(id);
        userRepository.delete(user);
        auditService.log("USER_DELETED", "USER", id, user.getEmail(), null);
    }

    @Transactional
    public UserResponse activate(Long id) {
        User user = findUser(id);
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);
        return MapperUtils.toUserResponse(user);
    }

    @Transactional
    public UserResponse deactivate(Long id) {
        User user = findUser(id);
        user.setStatus(UserStatus.INACTIVE);
        user = userRepository.save(user);
        return MapperUtils.toUserResponse(user);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = findUser(id);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        auditService.log("PASSWORD_RESET", "USER", id, null, null);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }
}
