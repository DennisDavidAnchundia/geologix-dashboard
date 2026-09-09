package com.geologix.web;

import com.geologix.dto.LoginRequest;
import com.geologix.dto.LoginResponse;
import com.geologix.model.Role;
import com.geologix.model.User;
import com.geologix.repository.UserRepository;
import com.geologix.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;

/**
 * Autenticación del panel: login y perfil propio.
 * El registro de usuarios nuevos queda reservado a ADMIN.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /** Login con usuario/clave → devuelve JWT. */
    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));
        if (!user.isEnabled() || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }
        return new LoginResponse(jwtService.generate(user.getUsername(), user.getRole()),
                user.getUsername(), user.getRole().name());
    }

    /** Perfil del usuario autenticado (para el frontend). */
    @GetMapping("/me")
    public LoginResponse me(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return new LoginResponse(null, user.getUsername(), user.getRole().name());
    }

    /** Crear usuario (solo ADMIN). */
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public LoginResponse register(@RequestBody RegisterRequest req) {
        if (userRepository.existsByUsername(req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya existe");
        }
        Role role;
        try {
            role = Role.valueOf(req.role());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol inválido (ADMIN u OPERADOR)");
        }
        User user = User.builder()
                .username(req.username())
                .password(passwordEncoder.encode(req.password()))
                .role(role)
                .enabled(true)
                .createdAt(Instant.now())
                .build();
        userRepository.save(user);
        return new LoginResponse(null, user.getUsername(), user.getRole().name());
    }

    public record RegisterRequest(String username, String password, String role) {
    }
}
