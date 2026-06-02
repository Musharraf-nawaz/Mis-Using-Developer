package com.aims.config;

import com.aims.entity.User;
import com.aims.entity.enums.Role;
import com.aims.entity.enums.UserStatus;
import com.aims.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            log.info("Initializing default users...");
            createUser("System Administrator", "admin@aims.com", Role.ADMIN, "IT", "EMP001");
            createUser("HR Manager", "hr@aims.com", Role.HR, "Human Resources", "EMP002");
            createUser("John Employee", "employee@aims.com", Role.EMPLOYEE, "Engineering", "EMP003");
            log.info("Default users created. Password: Admin@123");
        }
    }

    private void createUser(String name, String email, Role role, String dept, String empId) {
        userRepository.save(User.builder()
                .fullName(name)
                .email(email)
                .password(passwordEncoder.encode("Admin@123"))
                .role(role)
                .department(dept)
                .employeeId(empId)
                .status(UserStatus.ACTIVE)
                .build());
    }
}
