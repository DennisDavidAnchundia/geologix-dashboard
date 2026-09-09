package com.geologix.dto;

/** Respuesta de login con el token y el rol. */
public record LoginResponse(String token, String username, String role) {
}
