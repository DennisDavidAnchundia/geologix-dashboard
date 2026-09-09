package com.geologix.service;

import com.geologix.converter.EntityDtoConverter;
import com.geologix.dto.GeofenceDto;
import com.geologix.dto.GeofenceRequest;
import com.geologix.model.Geofence;
import com.geologix.model.ZonaTipo;
import com.geologix.repository.GeofenceRepository;
import org.geolatte.geom.G2D;
import org.geolatte.geom.Polygon;
import org.geolatte.geom.crs.CoordinateReferenceSystems;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.geolatte.geom.builder.DSL.g;
import static org.geolatte.geom.builder.DSL.polygon;
import static org.geolatte.geom.builder.DSL.ring;
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

    // ---------- CRUD (Pack Altura 5.6) ----------

    public GeofenceDto crear(GeofenceRequest req) {
        Geofence gf = Geofence.builder()
                .nombre(validarNombre(req.nombre()))
                .tipo(validarTipo(req.tipo()))
                .geom(validarYConstruirPoligono(req.coordenadas()))
                .color(validarColor(req.color()))
                .activa(req.activa() == null || req.activa())
                .build();
        return converter.toGeofenceDto(geofenceRepository.save(gf));
    }

    public GeofenceDto actualizar(Long id, GeofenceRequest req) {
        Geofence gf = buscar(id);
        gf.setNombre(validarNombre(req.nombre()));
        gf.setTipo(validarTipo(req.tipo()));
        gf.setGeom(validarYConstruirPoligono(req.coordenadas()));
        if (req.color() != null) {
            gf.setColor(validarColor(req.color()));
        }
        if (req.activa() != null) {
            gf.setActiva(req.activa());
        }
        limpiarEstadoMemoria(id);
        return converter.toGeofenceDto(geofenceRepository.save(gf));
    }

    public void eliminar(Long id) {
        Geofence gf = buscar(id);
        geofenceRepository.delete(gf);
        limpiarEstadoMemoria(id);
    }

    public GeofenceDto obtener(Long id) {
        return converter.toGeofenceDto(buscar(id));
    }

    public GeofenceDto cambiarActiva(Long id, boolean activa) {
        Geofence gf = buscar(id);
        gf.setActiva(activa);
        limpiarEstadoMemoria(id);
        return converter.toGeofenceDto(geofenceRepository.save(gf));
    }

    private Geofence buscar(Long id) {
        return geofenceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Geofence no encontrada con id " + id));
    }

    /** Al editar/borrar una zona, el estado en memoria queda obsoleto → se purga. */
    private void limpiarEstadoMemoria(Long geofenceId) {
        estadoVehiculoGeofence.keySet().removeIf(k -> k.endsWith(":" + geofenceId));
    }

    private String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank() || nombre.trim().length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nombre requerido (1-100 caracteres)");
        }
        return nombre.trim();
    }

    private ZonaTipo validarTipo(String tipo) {
        if (tipo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo requerido (ALMACEN, ZONA_REPARTO, RESTRINGIDA, COBERTURA)");
        }
        try {
            return ZonaTipo.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo inválido: " + tipo);
        }
    }

    private String validarColor(String color) {
        if (color == null || color.isBlank()) {
            return "#38BDF8";
        }
        if (!color.trim().matches("^#[0-9A-Fa-f]{6}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Color inválido (formato #RRGGBB)");
        }
        return color.trim();
    }

    /**
     * Valida el anillo [lon,lat] y lo convierte a Polygon WGS84.
     * Mínimo 3 vértices distintos; se cierra automáticamente.
     */
    private Polygon validarYConstruirPoligono(java.util.List<java.util.List<Double>> coords) {
        if (coords == null || coords.size() < 3) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se requieren al menos 3 puntos [lon, lat]");
        }
        var puntos = new java.util.ArrayList<G2D>();
        for (var par : coords) {
            if (par == null || par.size() != 2 || par.get(0) == null || par.get(1) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cada punto debe ser [lon, lat]");
            }
            double lon = par.get(0);
            double lat = par.get(1);
            if (lon < -180 || lon > 180 || lat < -90 || lat > 90) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Coordenada fuera de rango: [" + lon + ", " + lat + "]");
            }
            puntos.add(new G2D(lon, lat));
        }
        // Cerrar el anillo si no está cerrado.
        if (!puntos.get(0).equals(puntos.get(puntos.size() - 1))) {
            puntos.add(puntos.get(0));
        }
        if (puntos.size() < 4) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se requieren al menos 3 vértices distintos");
        }
        try {
            return polygon(CoordinateReferenceSystems.WGS84, ring(puntos.toArray(new G2D[0])));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Polígono inválido: " + e.getMessage());
        }
    }

    /** Resultado de una transición geofence detectada. */
    public record TransicionGeofence(
            Geofence geofence,
            String tipo,
            String mensaje
    ) {
    }
}
