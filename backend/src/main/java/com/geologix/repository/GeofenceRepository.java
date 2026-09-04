package com.geologix.repository;

import com.geologix.model.Geofence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GeofenceRepository extends JpaRepository<Geofence, Long> {

    /** Devuelve todas las geofences activas. */
    List<Geofence> findByActivaTrue();

    /**
     * Verifica si un punto dado se encuentra dentro de alguna geofence activa.
     * Usa la función PostGIS {@code ST_Within} para comprobación espacial real.
     *
     * @param longitude coordenada X (longitud WGS84)
     * @param latitude  coordenada Y (latitud WGS84)
     * @return true si el punto está dentro de al menos una geofence activa
     */
    @Query(value = "SELECT EXISTS ("
            + "SELECT 1 FROM geofences "
            + "WHERE activa = true "
            + "AND ST_Within("
            + "  ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geometry, "
            + "  geom"
            + ")"
            + ")", nativeQuery = true)
    boolean estaDentroDeZona(@Param("lon") double longitude, @Param("lat") double latitude);

    /**
     * Devuelve las geofences activas que contienen el punto dado.
     * Útil para saber en qué zona(s) se encuentra un vehículo.
     */
    @Query(value = "SELECT * FROM geofences "
            + "WHERE activa = true "
            + "AND ST_Within("
            + "  ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geometry, "
            + "  geom"
            + ")", nativeQuery = true)
    List<Geofence> findZonasQueContienen(@Param("lon") double longitude, @Param("lat") double latitude);
}
