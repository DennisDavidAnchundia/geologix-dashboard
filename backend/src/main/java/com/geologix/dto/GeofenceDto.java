package com.geologix.dto;

import java.util.List;

/**
 * Datos de una geofence expuestos a través de la API.
 */
public record GeofenceDto(
        Long id,
        String nombre,
        String tipo,
        List<List<Double>> coordenadas,
        String color,
        boolean activa
) {
}
