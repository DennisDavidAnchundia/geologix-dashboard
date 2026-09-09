package com.geologix.web;

import com.geologix.converter.EntityDtoConverter;
import com.geologix.dto.AlertDto;
import com.geologix.repository.AlertRepository;
import com.geologix.service.AlertService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Expone las alertas generadas por el sistema a través de una API REST.
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertRepository alertRepository;
    private final AlertService alertService;
    private final EntityDtoConverter converter;

    public AlertController(AlertRepository alertRepository, AlertService alertService,
                           EntityDtoConverter converter) {
        this.alertRepository = alertRepository;
        this.alertService = alertService;
        this.converter = converter;
    }

    /**
     * Devuelve las alertas (más recientes primero). Con {@code activa=true} sólo
     * las no resueltas. {@code limit} acota el resultado (1-200, por defecto 50).
     */
    @GetMapping
    public List<AlertDto> list(@RequestParam(defaultValue = "false") boolean activa,
                               @RequestParam(defaultValue = "50") int limit) {
        int n = Math.min(Math.max(limit, 1), 200);
        var alerts = activa
                ? alertRepository.findByResueltaFalseOrderByTimestampDesc()
                : alertRepository.findAllByOrderByTimestampDesc();
        return alerts.stream().limit(n).map(converter::toAlertDto).toList();
    }

    /** Marca una alerta como resuelta (cualquier rol autenticado: trabajo de operador). */
    @PatchMapping("/{id}/resolver")
    public AlertDto resolver(@PathVariable Long id) {
        return alertService.resolver(id);
    }

    /** Resuelve todas las activas de una vez. */
    @PatchMapping("/resolver-todas")
    public java.util.Map<String, Long> resolverTodas() {
        return java.util.Map.of("resueltas", alertService.resolverTodas());
    }

    /** Conteo real (sin tope) para badges y KPIs. */
    @GetMapping("/count")
    public java.util.Map<String, Long> count(@RequestParam(defaultValue = "false") boolean activa) {
        long n = activa ? alertRepository.countByResueltaFalse() : alertRepository.count();
        return java.util.Map.of("count", n);
    }
}
