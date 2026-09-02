package com.geologix.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Una posición geográfica registrada por el GPS de un vehículo en un instante dado.
 */
@Entity
@Table(name = "positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Vehículo al que pertenece esta posición. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    /** Latitud (WGS84). */
    @Column(nullable = false)
    private double latitude;

    /** Longitud (WGS84). */
    @Column(nullable = false)
    private double longitude;

    /** Velocidad al momento del registro, en km/h. */
    @Column(nullable = false)
    private double velocidadKmh;

    /** Estado del motor (encendido/apagado). */
    @Column(nullable = false)
    private boolean ignition;

    /** Instante en que el dispositivo GPS registró la posición. */
    @Column(nullable = false, updatable = false)
    private Instant timestamp;
}
