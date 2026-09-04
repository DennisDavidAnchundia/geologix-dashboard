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
import org.geolatte.geom.Polygon;
import org.geolatte.geom.crs.CoordinateReferenceSystems;

import jakarta.persistence.PrePersist;

import static org.geolatte.geom.builder.DSL.g;
import static org.geolatte.geom.builder.DSL.polygon;

/**
 * Una zona geográfica poligonal (geofence) utilizada para controlar
 * el acceso de vehículos a áreas específicas.
 *
 * <p>Cada geofence tiene un polígono en WGS84 (SRID 4326) que define
 * su perímetro. El sistema verifica en tiempo real si los vehículos
 * se encuentran dentro o fuera de estas zonas.
 */
@Entity
@Table(name = "geofences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Geofence {

    public static final int SRID = 4326;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre descriptivo de la zona. */
    @Column(nullable = false, length = 100)
    private String nombre;

    /** Tipo de zona geográfica. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ZonaTipo tipo;

    /** Polígono que define la zona (WGS84, SRID 4326). */
    @Column(columnDefinition = "geometry(Polygon,4326)", nullable = false)
    private Polygon geom;

    /** Color hexadecimal para visualización en el mapa. */
    @Column(length = 7)
    private String color;

    /** Indica si la geofence está activa para monitoreo. */
    @Column(nullable = false)
    private boolean activa;

    @PrePersist
    private void inicializar() {
        if (this.color == null) {
            this.color = "#38BDF8";
        }
        if (!this.activa) {
            this.activa = true;
        }
    }
}
