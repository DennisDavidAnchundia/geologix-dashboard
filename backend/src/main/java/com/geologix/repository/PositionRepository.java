package com.geologix.repository;

import com.geologix.model.Position;
import com.geologix.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PositionRepository extends JpaRepository<Position, Long> {

    /** Devuelve el historial de posiciones de un vehículo, de la más reciente a la más antigua. */
    List<Position> findByVehicleOrderByTimestampDesc(Vehicle vehicle);

    /**
     * Distancia total recorrida (km) por un vehículo, calculada con PostGIS.
     *
     * <p>Une todas las posiciones del vehículo ({@code ST_MakeLine}) en orden cronológico
     * y mide la longitud de la línea resultante ({@code ST_Length}). Requiere la extensión
     * PostGIS y el perfil 'postgis'; no funciona sobre H2.
     */
    @Query(value = "SELECT COALESCE("
            + "ST_Length(ST_MakeLine(geom ORDER BY timestamp)::geography) / 1000.0, 0.0) "
            + "FROM positions WHERE vehicle_id = :vehicleId", nativeQuery = true)
    double distanciaRecorridaKm(@Param("vehicleId") Long vehicleId);
}
