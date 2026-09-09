package com.geologix.dto;

import java.util.List;

/**
 * Petición para crear/actualizar una geofence.
 * Coordenadas en formato [lon, lat] (orden PostGIS).
 */
public record GeofenceRequest(
        String nombre,
        String tipo,
        List<List<Double>> coordenadas,
        String color,
        Boolean activa
) {
}
