package com.geologix.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Representa un vehículo de la flota monitoreada (por ejemplo, un camión de reparto).
 */
@Entity
@Table(name = "vehicles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Placa identificatoria del vehículo. */
    @Column(nullable = false, unique = true, length = 20)
    private String placa;

    /** Nombre del conductor responsable. */
    @Column(nullable = false, length = 100)
    private String conductor;

    /** Estado operativo del vehículo. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleStatus estado;

    /** Momento en que se registró el vehículo en el sistema. */
    @Column(nullable = false, updatable = false)
    private Instant creadoEn;
}
