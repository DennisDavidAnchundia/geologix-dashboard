package com.geologix.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.geolatte.geom.G2D;
import org.geolatte.geom.Point;
import org.geolatte.geom.crs.CoordinateReferenceSystems;

import java.time.Instant;

import static org.geolatte.geom.builder.DSL.g;
import static org.geolatte.geom.builder.DSL.point;

/**
 * Una posición geográfica registrada por el GPS de un vehículo en un instante dado.
 *
 * <p>Además de las coordenadas simples (lat/lon, usadas por la API y el frontend), se
 * persiste una columna espacial {@code geom} de tipo {@code geometry(Point,4326)}
 * gestionada por Hibernate Spatial. Esta geometría permite consultas PostGIS reales
 * (distancia recorrida, contención en zonas, etc.).
 */
@Entity
@Table(name = "positions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position {

    public static final int SRID = 4326;

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

    /** Punto espacial (SRID 4326) sincronizado con lat/lon para consultas PostGIS. */
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point geom;

    /** Velocidad al momento del registro, en km/h. */
    @Column(nullable = false)
    private double velocidadKmh;

    /** Estado del motor (encendido/apagado). */
    @Column(nullable = false)
    private boolean ignition;

    /** Instante en que el dispositivo GPS registró la posición. */
    @Column(nullable = false, updatable = false)
    private Instant timestamp;

    /** Mantiene la geometría espacial sincronizada con las coordenadas antes de persistir. */
    @PrePersist
    @PreUpdate
    private void sincronizarGeometria() {
        if (this.geom == null) {
            G2D coordenada = g(longitude, latitude);
            this.geom = point(CoordinateReferenceSystems.WGS84, coordenada);
        }
    }
}
