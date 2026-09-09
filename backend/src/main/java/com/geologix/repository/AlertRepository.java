package com.geologix.repository;

import com.geologix.model.Alert;
import com.geologix.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepository extends JpaRepository<Alert, Long> {

    /** Alertas de un vehículo, ordenadas de la más reciente a la más antigua. */
    List<Alert> findByVehicleOrderByTimestampDesc(Vehicle vehicle);

    /** Alertas no resueltas (activas). */
    List<Alert> findByResueltaFalseOrderByTimestampDesc();

    /** Conteo de alertas no resueltas (para badges/KPIs sin traer la lista). */
    long countByResueltaFalse();

    /** Todas las alertas, ordenadas de la más reciente a la más antigua. */
    List<Alert> findAllByOrderByTimestampDesc();
}
