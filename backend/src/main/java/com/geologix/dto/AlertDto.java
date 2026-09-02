package com.geologix.dto;

import java.time.Instant;

/**
 * Datos de una alerta expuestos a través de la API.
 */
public record AlertDto(
        Long id,
        Long vehicleId,
        String tipo,
        String severidad,
        String mensaje,
        boolean resuelta,
        Instant timestamp
) {
}
