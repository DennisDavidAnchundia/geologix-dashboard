package com.geologix.dto;

import java.time.Instant;

/**
 * Datos de un vehículo expuestos a través de la API, incluida su última posición conocida.
 * Resulta útil para el mapa del frontend (muestra directamente dónde está cada vehículo).
 */
public record VehicleDto(
        Long id,
        String placa,
        String conductor,
        String estado,
        PositionDto ultimaPosicion,
        Instant creadoEn
) {
}
