package com.geologix.web;

import com.geologix.converter.EntityDtoConverter;
import com.geologix.dto.AlertDto;
import com.geologix.repository.AlertRepository;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final EntityDtoConverter converter;

    public AlertController(AlertRepository alertRepository, EntityDtoConverter converter) {
        this.alertRepository = alertRepository;
        this.converter = converter;
    }

    /**
     * Devuelve las alertas. Con {@code activa=true} sólo se devuelven las no resueltas.
     */
    @GetMapping
    public List<AlertDto> list(@RequestParam(defaultValue = "false") boolean activa) {
        var alerts = activa
                ? alertRepository.findByResueltaFalseOrderByTimestampDesc()
                : alertRepository.findAllByOrderByTimestampDesc();
        return alerts.stream().map(converter::toAlertDto).toList();
    }
}
