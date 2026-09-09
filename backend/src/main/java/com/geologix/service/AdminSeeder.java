package com.geologix.service;

import com.geologix.model.Role;
import com.geologix.model.User;
import com.geologix.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Crea los usuarios iniciales si la tabla está vacía:
 * admin (ADMIN) y operador (OPERADOR).
 */
@Component
@Order(1)
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        crearSiNoExiste("admin", "geologix123", Role.ADMIN);
        crearSiNoExiste("operador", "geologix123", Role.OPERADOR);
    }

    private void crearSiNoExiste(String username, String clave, Role role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        userRepository.save(User.builder()
                .username(username)
                .password(passwordEncoder.encode(clave))
                .role(role)
                .enabled(true)
                .createdAt(Instant.now())
                .build());
    }
}
