package com.geologix.converter;

import com.geologix.dto.AlertDto;
import com.geologix.dto.GeofenceDto;
import com.geologix.dto.PositionDto;
import com.geologix.dto.VehicleDto;
import com.geologix.model.Alert;
import com.geologix.model.Geofence;
import com.geologix.model.Position;
import com.geologix.model.Vehicle;
import com.geologix.repository.PositionRepository;
import org.geolatte.geom.G2D;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Convierte entidades del dominio en DTO para no exponer detalles internos de la capa de datos.
 */
@Component
public class EntityDtoConverter {

    private final PositionRepository positionRepository;

    public EntityDtoConverter(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    public PositionDto toPositionDto(Position p) {
        return new PositionDto(
                p.getId(),
                p.getVehicle().getId(),
                p.getLatitude(),
                p.getLongitude(),
                p.getVelocidadKmh(),
                p.isIgnition(),
                p.getTimestamp()
        );
    }

    public AlertDto toAlertDto(Alert a) {
        return new AlertDto(
                a.getId(),
                a.getVehicle().getId(),
                a.getTipo().name(),
                a.getSeveridad().name(),
                a.getMensaje(),
                a.isResuelta(),
                a.getTimestamp()
        );
    }

    public VehicleDto toVehicleDto(Vehicle v) {
        PositionDto ultima = positionRepository
                .findByVehicleOrderByTimestampDesc(v)
                .stream().findFirst()
                .map(this::toPositionDto)
                .orElse(null);

        return new VehicleDto(
                v.getId(),
                v.getPlaca(),
                v.getConductor(),
                v.getEstado().name(),
                ultima,
                v.getCreadoEn()
        );
    }

    public GeofenceDto toGeofenceDto(Geofence g) {
        List<List<Double>> coordenadas = new ArrayList<>();
        if (g.getGeom() != null) {
            var anillo = g.getGeom().getExteriorRing();
            for (int i = 0; i < anillo.getNumPositions(); i++) {
                var pos = (G2D) anillo.getPositionN(i);
                coordenadas.add(List.of(pos.getLon(), pos.getLat()));
            }
            // Cerrar el polígono si no está cerrado
            if (!coordenadas.isEmpty() && !coordenadas.get(0).equals(coordenadas.get(coordenadas.size() - 1))) {
                coordenadas.add(coordenadas.get(0));
            }
        }

        return new GeofenceDto(
                g.getId(),
                g.getNombre(),
                g.getTipo().name(),
                coordenadas,
                g.getColor(),
                g.isActiva()
        );
    }
}
