package com.scaler.usermanagement.config;

import com.scaler.usermanagement.enums.RoleName;
import com.scaler.usermanagement.model.Role;
import com.scaler.usermanagement.model.User;
import com.scaler.usermanagement.model.UserCredential;
import com.scaler.usermanagement.repository.RoleRepository;
import com.scaler.usermanagement.repository.UserCredentialRepository;
import com.scaler.usermanagement.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Configuration
@ConditionalOnProperty(name = "security.admin-bootstrap.enabled", havingValue = "true")
public class AdminBootstrapRunner {

    @Bean
    public ApplicationRunner adminBootstrap(UserRepository userRepository,
            UserCredentialRepository credentialRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder, Environment env) {
        return args -> bootstrap(userRepository, credentialRepository, roleRepository, passwordEncoder, env);
    }

    @Transactional
    void bootstrap(UserRepository userRepository, UserCredentialRepository credentialRepository,
            RoleRepository roleRepository, PasswordEncoder passwordEncoder, Environment env) {
        String email = env.getProperty("security.admin-bootstrap.email", "").trim().toLowerCase();
        String password = env.getProperty("security.admin-bootstrap.password", "");
        if (!StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            return;
        }
        if (userRepository.existsByEmail(email)) {
            return;
        }
        User admin = new User();
        admin.setEmail(email);
        admin.setFirstName("Admin");
        admin.setLastName("User");
        Role adminRole = roleRepository.findByName(RoleName.ADMIN.name())
                .orElseThrow(() -> new IllegalStateException("ADMIN role missing"));
        admin.getRoles().add(adminRole);
        userRepository.save(admin);

        UserCredential credential = new UserCredential();
        credential.setUserId(admin.getId());
        credential.setPasswordHash(passwordEncoder.encode(password));
        credentialRepository.save(credential);
    }
}
