package com.geologix.web;

import com.geologix.dto.GeofenceDto;
import com.geologix.service.GeofenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone las geofences (zonas geográficas) a través de una API REST.
 */
@RestController
@RequestMapping("/api/geofences")
public class GeofenceController {

    private final GeofenceService geofenceService;

    public GeofenceController(GeofenceService geofenceService) {
        this.geofenceService = geofenceService;
    }

    /** Devuelve todas las geofences registradas. */
    @GetMapping
    public List<GeofenceDto> list() {
        return geofenceService.findAll();
    }
}
