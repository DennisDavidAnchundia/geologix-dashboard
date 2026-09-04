package com.geologix.service;

import com.geologix.model.Geofence;
import com.geologix.model.ZonaTipo;
import com.geologix.repository.GeofenceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import org.geolatte.geom.Polygon;
import org.geolatte.geom.crs.CoordinateReferenceSystems;

import static org.geolatte.geom.builder.DSL.g;
import static org.geolatte.geom.builder.DSL.polygon;
import static org.geolatte.geom.builder.DSL.ring;

/**
 * Precarga geofences de demostración al arrancar la aplicación.
 * Define zonas alrededor de Guayaquil para simular áreas de reparto,
 * almacenes y zonas restringidas.
 *
 * <p>Importante: en PostGIS el orden es (longitud, latitud), igual que el
 * resto de geometrías del proyecto (ver {@link com.geologix.model.Position}).
 */
@Component
public class GeofenceSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GeofenceSeeder.class);

    private final GeofenceRepository geofenceRepository;

    public GeofenceSeeder(GeofenceRepository geofenceRepository) {
        this.geofenceRepository = geofenceRepository;
    }

    @Override
    public void run(String... args) {
        if (geofenceRepository.count() > 0) {
            log.info("Geofences ya inicializadas ({}). Se omite seed.", geofenceRepository.count());
            return;
        }

        crearZonaAlmacen();
        crearZonaRepartoNorte();
        crearZonaRepartoSur();
        crearZonaRestringida();
        crearZonaCobertura();

        log.info("Geofences de demostración creadas: 5 zonas alrededor de Guayaquil.");
    }

    /** Almacén central en zona urbana de Guayaquil. */
    private void crearZonaAlmacen() {
        Polygon poly = polygon(CoordinateReferenceSystems.WGS84,
                ring(
                        g(-79.8920, -2.1920),
                        g(-79.8920, -2.1880),
                        g(-79.8880, -2.1880),
                        g(-79.8880, -2.1920),
                        g(-79.8920, -2.1920)
                )
        );
        geofenceRepository.save(Geofence.builder()
                .nombre("Almacen Central")
                .tipo(ZonaTipo.ALMACEN)
                .geom(poly)
                .color("#10B981")
                .activa(true)
                .build());
    }

    /** Zona de reparto norte. */
    private void crearZonaRepartoNorte() {
        Polygon poly = polygon(CoordinateReferenceSystems.WGS84,
                ring(
                        g(-79.9400, -2.1600),
                        g(-79.9400, -2.1450),
                        g(-79.9250, -2.1450),
                        g(-79.9250, -2.1600),
                        g(-79.9400, -2.1600)
                )
        );
        geofenceRepository.save(Geofence.builder()
                .nombre("Zona Reparto Norte")
                .tipo(ZonaTipo.ZONA_REPARTO)
                .geom(poly)
                .color("#3B82F6")
                .activa(true)
                .build());
    }

    /** Zona de reparto sur. */
    private void crearZonaRepartoSur() {
        Polygon poly = polygon(CoordinateReferenceSystems.WGS84,
                ring(
                        g(-79.9200, -2.2300),
                        g(-79.9200, -2.2150),
                        g(-79.9050, -2.2150),
                        g(-79.9050, -2.2300),
                        g(-79.9200, -2.2300)
                )
        );
        geofenceRepository.save(Geofence.builder()
                .nombre("Zona Reparto Sur")
                .tipo(ZonaTipo.ZONA_REPARTO)
                .geom(poly)
                .color("#F59E0B")
                .activa(true)
                .build());
    }

    /** Zona restringida (puerto marítimo). */
    private void crearZonaRestringida() {
        Polygon poly = polygon(CoordinateReferenceSystems.WGS84,
                ring(
                        g(-79.9100, -2.2800),
                        g(-79.9100, -2.2700),
                        g(-79.9000, -2.2700),
                        g(-79.9000, -2.2800),
                        g(-79.9100, -2.2800)
                )
        );
        geofenceRepository.save(Geofence.builder()
                .nombre("Puerto Maritimo")
                .tipo(ZonaTipo.RESTRINGIDA)
                .geom(poly)
                .color("#EF4444")
                .activa(true)
                .build());
    }

    /** Zona de cobertura amplia (área metropolitana). */
    private void crearZonaCobertura() {
        Polygon poly = polygon(CoordinateReferenceSystems.WGS84,
                ring(
                        g(-79.9700, -2.2200),
                        g(-79.9700, -2.1400),
                        g(-79.8500, -2.1400),
                        g(-79.8500, -2.2200),
                        g(-79.9700, -2.2200)
                )
        );
        geofenceRepository.save(Geofence.builder()
                .nombre("Cobertura Guayaquil")
                .tipo(ZonaTipo.COBERTURA)
                .geom(poly)
                .color("#8B5CF6")
                .activa(true)
                .build());
    }
}
