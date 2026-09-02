package com.geologix.dto;

import java.time.Instant;

/**
 * Datos de una posición GPS expuestos a través de la API.
 */
public record PositionDto(
        Long id,
        Long vehicleId,
        double latitude,
        double longitude,
        double velocidadKmh,
        boolean ignition,
        Instant timestamp
) {
}
