package com.geologix.service;

import com.geologix.converter.EntityDtoConverter;
import com.geologix.dto.AlertDto;
import com.geologix.dto.PositionDto;
import com.geologix.model.Alert;
import com.geologix.model.Position;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class RealtimePublisher {

    private static final Logger log = LoggerFactory.getLogger(RealtimePublisher.class);

    private final SimpMessagingTemplate messaging;
    private final EntityDtoConverter converter;

    public RealtimePublisher(SimpMessagingTemplate messaging, EntityDtoConverter converter) {
        this.messaging = messaging;
        this.converter = converter;
    }

    public void publicarPosicion(Position position) {
        PositionDto dto = converter.toPositionDto(position);
        messaging.convertAndSend("/topic/positions", dto);
        log.debug("WS emitida posición → vehicleId={}, lat={}, lon={}",
                position.getVehicle().getId(), position.getLatitude(), position.getLongitude());
    }

    public void publicarAlerta(Alert alert) {
        AlertDto dto = converter.toAlertDto(alert);
        messaging.convertAndSend("/topic/alerts", dto);
        log.debug("WS emitida alerta → tipo={}, vehicleId={}", alert.getTipo(), alert.getVehicle().getId());
    }
}
