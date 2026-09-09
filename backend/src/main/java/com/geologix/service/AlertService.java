package com.geologix.service;

import com.geologix.config.SimulationProperties;
import com.geologix.converter.EntityDtoConverter;
import com.geologix.dto.AlertDto;
import com.geologix.model.Alert;
import com.geologix.model.AlertSeverity;
import com.geologix.model.AlertType;
import com.geologix.model.Position;
import com.geologix.model.Vehicle;
import com.geologix.repository.AlertRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Evalúa las reglas operativas sobre las posiciones recibidas y genera alertas
 * cuando se detectan condiciones anómalas (exceso de velocidad, detención prolongada,
 * entrada/salida de geofences).
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final AlertRepository alertRepository;
    private final RealtimePublisher publisher;
    private final SimulationProperties properties;
    private final GeofenceService geofenceService;
    private final EntityDtoConverter converter;

    public AlertService(AlertRepository alertRepository, RealtimePublisher publisher,
                        SimulationProperties properties, GeofenceService geofenceService,
                        EntityDtoConverter converter) {
        this.alertRepository = alertRepository;
        this.publisher = publisher;
        this.properties = properties;
        this.geofenceService = geofenceService;
        this.converter = converter;
    }

    /**
     * Evalúa todas las reglas para una posición recién registrada y crea las alertas
     * que correspondan. Cada tipo de alerta sólo se crea si no existe ya una activa
     * del mismo tipo para ese vehículo (se evita saturar de alertas repetidas).
     */
    public void evaluar(Position position) {
        evaluarExcesoVelocidad(position);
        evaluarDetencionProlongada(position);
        evaluarGeofences(position);
    }

    /**
     * Evalúa las transiciones de geofences para un vehículo y genera alertas
     * de ingreso/salida cuando el vehículo cruza los límites de una zona.
     */
    private void evaluarGeofences(Position position) {
        List<GeofenceService.TransicionGeofence> transiciones =
                geofenceService.evaluarTransiciones(
                        position.getVehicle().getId(),
                        position.getLatitude(),
                        position.getLongitude()
                );

        for (GeofenceService.TransicionGeofence t : transiciones) {
            AlertType tipo = "INGRESO".equals(t.tipo())
                    ? AlertType.INGRESO_ZONA
                    : AlertType.SALIDA_ZONA;
            AlertSeverity severidad = "INGRESO".equals(t.tipo())
                    ? AlertSeverity.BAJA
                    : AlertSeverity.MEDIA;

            // Las alertas quedan ACTIVAS hasta que un operador las atiende (Resolver).
            // "Activas" = pendientes de atender: el contador sube con cada novedad.
            crear(position.getVehicle(), tipo, severidad, t.mensaje());
        }
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

    /** Marca una alerta como resuelta (trabajo de operador). */
    public AlertDto resolver(Long id) {
        Alert alerta = alertRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Alerta no encontrada con id " + id));
        alerta.setResuelta(true);
        return converter.toAlertDto(alertRepository.save(alerta));
    }

    /** Resuelve TODAS las activas de una vez (cambio de turno / limpieza). Devuelve cuántas fueron. */
    public long resolverTodas() {
        var activas = alertRepository.findByResueltaFalseOrderByTimestampDesc();
        activas.forEach(a -> a.setResuelta(true));
        alertRepository.saveAll(activas);
        return activas.size();
    }

    /** Al borrar una zona, se cierran sus alertas pendientes (si no, quedan huérfanas para siempre). */
    public long resolverPorZona(String nombreZona) {
        var huerfanas = alertRepository.findByResueltaFalseOrderByTimestampDesc().stream()
                .filter(a -> a.getMensaje() != null && a.getMensaje().contains(nombreZona))
                .toList();
        huerfanas.forEach(a -> a.setResuelta(true));
        alertRepository.saveAll(huerfanas);
        return huerfanas.size();
    }
}
