package com.event.authservice.config;

import com.event.authservice.entity.Role;
import com.event.authservice.entity.RoleName;
import com.event.authservice.entity.User;
import com.event.authservice.entity.UserStatus;
import com.event.authservice.repository.RoleRepository;
import com.event.authservice.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final AdminSeedProperties adminSeedProperties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(AdminSeedProperties adminSeedProperties,
                       UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder) {
        this.adminSeedProperties = adminSeedProperties;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!adminSeedProperties.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(adminSeedProperties.getEmail())
                || !StringUtils.hasText(adminSeedProperties.getPassword())
                || userRepository.existsByEmailIgnoreCase(adminSeedProperties.getEmail())) {
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role is missing"));

        User admin = new User();
        admin.setFullName(adminSeedProperties.getFullName());
        admin.setEmail(adminSeedProperties.getEmail().trim().toLowerCase());
        admin.setPasswordHash(passwordEncoder.encode(adminSeedProperties.getPassword()));
        admin.setStatus(UserStatus.ACTIVE);
        admin.setRoles(Set.of(adminRole));

        userRepository.save(admin);
    }
}
