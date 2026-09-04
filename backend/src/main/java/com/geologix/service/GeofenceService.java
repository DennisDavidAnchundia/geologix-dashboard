package com.geologix.service;

import com.geologix.converter.EntityDtoConverter;
import com.geologix.dto.GeofenceDto;
import com.geologix.model.Geofence;
import com.geologix.repository.GeofenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de gestión de geofences y detección de entrada/salida de vehículos.
 *
 * <p>Mantiene en memoria el estado de cada vehículo respecto a las geofences
 * (dentro/fuera de cada zona) para detectar transiciones y generar alertas
 * de ingreso/salida en tiempo real.
 */
@Service
public class GeofenceService {

    private static final Logger log = LoggerFactory.getLogger(GeofenceService.class);

    private final GeofenceRepository geofenceRepository;
    private final EntityDtoConverter converter;

    /**
     * Estado de cada vehículo respecto a cada geofence.
     * clave: "vehicleId:geofenceId" → true (dentro) / false (fuera).
     */
    private final Map<String, Boolean> estadoVehiculoGeofence = new ConcurrentHashMap<>();

    public GeofenceService(GeofenceRepository geofenceRepository, EntityDtoConverter converter) {
        this.geofenceRepository = geofenceRepository;
        this.converter = converter;
    }

    public List<GeofenceDto> findAll() {
        return geofenceRepository.findAll().stream()
                .map(converter::toGeofenceDto)
                .toList();
    }

    public List<GeofenceDto> findActivas() {
        return geofenceRepository.findByActivaTrue().stream()
                .map(converter::toGeofenceDto)
                .toList();
    }

    /**
     * Evalúa si un vehículo ha cruzado los límites de alguna geofence.
     * Devuelve una lista de transiciones detectadas: "INGRESO" o "SALIDA".
     *
     * @param vehicleId ID del vehículo
     * @param latitude  latitud actual
     * @param longitude longitud actual
     * @return lista de transiciones (cada una con geofence, tipo y mensaje)
     */
    public List<TransicionGeofence> evaluarTransiciones(Long vehicleId, double latitude, double longitude) {
        List<Geofence> zonasActuales = geofenceRepository.findZonasQueContienen(longitude, latitude);

        // Crear set de IDs de zonas actuales
        var zonasActualesIds = new java.util.HashSet<>(
                zonasActuales.stream().map(Geofence::getId).toList()
        );

        // Obtener todas las geofences activas para comparar
        List<Geofence> todasLasGeofences = geofenceRepository.findByActivaTrue();

        java.util.List<TransicionGeofence> transiciones = new java.util.ArrayList<>();

        for (Geofence gf : todasLasGeofences) {
            String clave = vehicleId + ":" + gf.getId();
            boolean estabaDentro = estadoVehiculoGeofence.getOrDefault(clave, false);
            boolean estaDentro = zonasActualesIds.contains(gf.getId());

            if (estaDentro && !estabaDentro) {
                // Ingresó a la zona
                transiciones.add(new TransicionGeofence(
                        gf, "INGRESO",
                        "Vehículo ingresó a zona: " + gf.getNombre()
                ));
                log.info("VEHÍCULO {} INGRESÓ a geofence '{}' ({})", vehicleId, gf.getNombre(), gf.getTipo());
            } else if (!estaDentro && estabaDentro) {
                // Salió de la zona
                transiciones.add(new TransicionGeofence(
                        gf, "SALIDA",
                        "Vehículo salió de zona: " + gf.getNombre()
                ));
                log.info("VEHÍCULO {} SALIÓ de geofence '{}' ({})", vehicleId, gf.getNombre(), gf.getTipo());
            }

            estadoVehiculoGeofence.put(clave, estaDentro);
        }

        return transiciones;
    }

    /**
     * Verifica si un punto está dentro de alguna geofence.
     */
    public boolean estaDentroDeZona(double longitude, double latitude) {
        return geofenceRepository.estaDentroDeZona(longitude, latitude);
    }

    /** Resultado de una transición geofence detectada. */
    public record TransicionGeofence(
            Geofence geofence,
            String tipo,
            String mensaje
    ) {
    }
}
