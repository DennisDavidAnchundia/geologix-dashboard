package com.geologix.service;

import com.geologix.config.SimulationProperties;
import com.geologix.model.Alert;
import com.geologix.model.AlertSeverity;
import com.geologix.model.AlertType;
import com.geologix.model.Position;
import com.geologix.model.Vehicle;
import com.geologix.repository.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Evalúa las reglas operativas sobre las posiciones recibidas y genera alertas
 * cuando se detectan condiciones anómalas (exceso de velocidad, detención prolongada).
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final RealtimePublisher publisher;
    private final SimulationProperties properties;

    public AlertService(AlertRepository alertRepository, RealtimePublisher publisher, SimulationProperties properties) {
        this.alertRepository = alertRepository;
        this.publisher = publisher;
        this.properties = properties;
    }

    /**
     * Evalúa todas las reglas para una posición recién registrada y crea las alertas
     * que correspondan. Cada tipo de alerta sólo se crea si no existe ya una activa
     * del mismo tipo para ese vehículo (se evita saturar de alertas repetidas).
     */
    public void evaluar(Position position) {
        evaluarExcesoVelocidad(position);
        evaluarDetencionProlongada(position);
    }

    private void evaluarExcesoVelocidad(Position position) {
        if (position.getVelocidadKmh() > properties.getSpeedLimitKmh()
                && noExisteActiva(position.getVehicle(), AlertType.EXCESO_VELOCIDAD)) {
            crear(position.getVehicle(), AlertType.EXCESO_VELOCIDAD, AlertSeverity.ALTA,
                    "Vehículo " + position.getVehicle().getPlaca() + " superó el límite de "
                            + (int) properties.getSpeedLimitKmh() + " km/h ("
                            + (int) position.getVelocidadKmh() + " km/h).");
        }
    }

    private void evaluarDetencionProlongada(Position position) {
        if (position.getVelocidadKmh() < properties.getStoppedThresholdKmh()
                && noExisteActiva(position.getVehicle(), AlertType.DETENCION_PROLONGADA)) {
            crear(position.getVehicle(), AlertType.DETENCION_PROLONGADA, AlertSeverity.MEDIA,
                    "Vehículo " + position.getVehicle().getPlaca() + " presenta una detención prolongada.");
        }
    }

    private boolean noExisteActiva(Vehicle vehicle, AlertType tipo) {
        return alertRepository.findByVehicleOrderByTimestampDesc(vehicle).stream()
                .noneMatch(a -> a.getTipo() == tipo && !a.isResuelta());
    }

    private void crear(Vehicle vehicle, AlertType tipo, AlertSeverity severidad, String mensaje) {
        Alert alerta = Alert.builder()
                .vehicle(vehicle)
                .tipo(tipo)
                .severidad(severidad)
                .mensaje(mensaje)
                .resuelta(false)
                .timestamp(Instant.now())
                .build();
        alertRepository.save(alerta);
        publisher.publicarAlerta(alerta);
        log.warn("ALERTA {} para {}: {}", tipo, vehicle.getPlaca(), mensaje);
    }

    /**
     * Devuelve la alerta activa (no resuelta) más reciente de un vehículo y tipo, si existe.
     */
    public Optional<Alert> findActiva(Vehicle vehicle, AlertType tipo) {
        return alertRepository.findByVehicleOrderByTimestampDesc(vehicle).stream()
                .filter(a -> a.getTipo() == tipo && !a.isResuelta())
                .findFirst();
    }
}
