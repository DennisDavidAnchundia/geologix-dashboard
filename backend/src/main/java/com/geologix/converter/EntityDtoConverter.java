package com.geologix.converter;

import com.geologix.dto.AlertDto;
import com.geologix.dto.PositionDto;
import com.geologix.dto.VehicleDto;
import com.geologix.model.Alert;
import com.geologix.model.Position;
import com.geologix.model.Vehicle;
import com.geologix.repository.PositionRepository;
import org.springframework.stereotype.Component;

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
}
