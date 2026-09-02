package com.geologix.repository;

import com.geologix.model.Position;
import com.geologix.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PositionRepository extends JpaRepository<Position, Long> {

    /** Devuelve el historial de posiciones de un vehículo, de la más reciente a la más antigua. */
    List<Position> findByVehicleOrderByTimestampDesc(Vehicle vehicle);
}
