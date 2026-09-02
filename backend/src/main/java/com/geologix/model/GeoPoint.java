package com.geologix.model;

/**
 * Punto geográfico simple (latitud, longitud) en coordenadas WGS84.
 * Se utiliza para definir los waypoints de las rutas.
 */
public record GeoPoint(double latitude, double longitude) {
}
