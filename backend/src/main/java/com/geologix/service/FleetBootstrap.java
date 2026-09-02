package com.geologix.service;

import com.geologix.config.SimulationProperties;
import com.geologix.model.Vehicle;
import com.geologix.model.VehicleStatus;
import com.geologix.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Al arrancar el sistema, crea la flota de vehículos de demostración si aún no existe.
 */
@Component
public class FleetBootstrap implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(FleetBootstrap.class);

    private static final List<String> PLACAS = List.of(
            "GXA-1001", "GXA-1002", "GXA-1003", "GXA-1004", "GXA-1005", "GXA-1006"
    );

    private static final List<String> CONDUCTORES = List.of(
            "Carlos Mena", "Lucía Andrade", "Pedro Salazar", "Ana Vega", "Jorge Ramos", "María Ponce"
    );

    private final VehicleRepository vehicleRepository;
    private final SimulationProperties properties;

    public FleetBootstrap(VehicleRepository vehicleRepository, SimulationProperties properties) {
        this.vehicleRepository = vehicleRepository;
        this.properties = properties;
    }

    @Override
    public void run(String... args) {
        int count = properties.getVehicleCount();
        if (vehicleRepository.count() > 0) {
            log.info("Flota ya inicializada ({} vehículos). Se omite el alta inicial.", vehicleRepository.count());
            return;
        }

        for (int i = 0; i < count; i++) {
            Vehicle vehicle = Vehicle.builder()
                    .placa(PLACAS.get(i % PLACAS.size()))
                    .conductor(CONDUCTORES.get(i % CONDUCTORES.size()))
                    .estado(VehicleStatus.DETENIDO)
                    .creadoEn(Instant.now())
                    .build();
            vehicleRepository.save(vehicle);
        }
        log.info("Flota inicializada con {} vehículos de demostración.", count);
    }
}
