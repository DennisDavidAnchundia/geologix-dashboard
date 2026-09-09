package com.geologix.web;

import com.geologix.dto.GeofenceDto;
import com.geologix.dto.GeofenceRequest;
import com.geologix.service.AlertService;
import com.geologix.service.GeofenceService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Expone las geofences (zonas geográficas) a través de una API REST.
 * Lectura: cualquier usuario autenticado. Mutaciones: solo ADMIN.
 */
@RestController
@RequestMapping("/api/geofences")
public class GeofenceController {

    private final GeofenceService geofenceService;
    private final AlertService alertService;

    public GeofenceController(GeofenceService geofenceService, AlertService alertService) {
        this.geofenceService = geofenceService;
        this.alertService = alertService;
    }

    /** Devuelve todas las geofences registradas. */
    @GetMapping
    public List<GeofenceDto> list() {
        return geofenceService.findAll();
    }

    /** Crea una zona nueva (solo ADMIN). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public GeofenceDto crear(@RequestBody GeofenceRequest req) {
        return geofenceService.crear(req);
    }

    /** Reemplaza una zona existente (solo ADMIN). */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public GeofenceDto actualizar(@PathVariable Long id, @RequestBody GeofenceRequest req) {
        return geofenceService.actualizar(id, req);
    }

    /** Activa/desactiva una zona sin borrarla (solo ADMIN). */
    @PatchMapping("/{id}/activa")
    @PreAuthorize("hasRole('ADMIN')")
    public GeofenceDto cambiarActiva(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean activa = body.get("activa");
        if (activa == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Se requiere el campo 'activa'");
        }
        return geofenceService.cambiarActiva(id, activa);
    }

    /** Elimina una zona (solo ADMIN) y cierra sus alertas pendientes. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public void eliminar(@PathVariable Long id) {
        String nombre = geofenceService.obtener(id).nombre();
        geofenceService.eliminar(id);
        alertService.resolverPorZona(nombre);
    }
}
