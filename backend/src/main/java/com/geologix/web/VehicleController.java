package com.geologix.web;

import com.geologix.converter.EntityDtoConverter;
import com.geologix.dto.PositionDto;
import com.geologix.dto.VehicleDto;
import com.geologix.model.Position;
import com.geologix.model.Vehicle;
import com.geologix.repository.PositionRepository;
import com.geologix.repository.VehicleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Expone los recursos de la flota de vehículos a través de una API REST.
 */
@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleRepository vehicleRepository;
    private final PositionRepository positionRepository;
    private final EntityDtoConverter converter;

    public VehicleController(VehicleRepository vehicleRepository,
                             PositionRepository positionRepository,
                             EntityDtoConverter converter) {
        this.vehicleRepository = vehicleRepository;
        this.positionRepository = positionRepository;
        this.converter = converter;
    }

    /** Devuelve la flota completa con su última posición conocida. */
    @GetMapping
    public List<VehicleDto> list() {
        return vehicleRepository.findAll().stream()
                .map(converter::toVehicleDto)
                .toList();
    }

    /** Devuelve un vehículo concreto con su última posición. */
    @GetMapping("/{id}")
    public VehicleDto get(@PathVariable Long id) {
        return converter.toVehicleDto(findVehicle(id));
    }

    /** Devuelve el historial de posiciones de un vehículo (más recientes primero). */
    @GetMapping("/{id}/positions")
    public List<PositionDto> positions(@PathVariable Long id) {
        Vehicle vehicle = findVehicle(id);
        return positionRepository.findByVehicleOrderByTimestampDesc(vehicle).stream()
                .map(converter::toPositionDto)
                .toList();
    }

    private Vehicle findVehicle(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Vehículo no encontrado con id " + id));
    }
}
