package com.geologix.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Una alerta generada por el sistema al detectar una condición anómala en un vehículo.
 */
@Entity
@Table(name = "alerts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Vehículo al que se asocia la alerta. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    /** Tipo de alerta (exceso de velocidad, detención prolongada, etc.). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AlertType tipo;

    /** Severidad asignada para priorizar la atención. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AlertSeverity severidad;

    /** Descripción legible de la alerta. */
    @Column(nullable = false, length = 255)
    private String mensaje;

    /** Indica si la alerta fue atendida/resuelta. */
    @Column(nullable = false)
    private boolean resuelta;

    /** Momento en que se generó la alerta. */
    @Column(nullable = false, updatable = false)
    private Instant timestamp;
}
