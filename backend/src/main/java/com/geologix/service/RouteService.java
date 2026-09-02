package com.geologix.service;

import com.geologix.model.GeoPoint;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Provee rutas de reparto predefinidas (waypoints geográficos) que los vehículos
 * recorren durante la simulación. Las coordenadas están ubicadas alrededor de
 * Guayaquil (Ecuador) para que el recorrido se vea realista y local.
 */
@Service
public class RouteService {

    private static final Map<Integer, List<GeoPoint>> ROUTES = Map.of(
            0, List.of(
                    new GeoPoint(-2.1894, -79.8891),
                    new GeoPoint(-2.1950, -79.8950),
                    new GeoPoint(-2.2020, -79.8900),
                    new GeoPoint(-2.2100, -79.8980),
                    new GeoPoint(-2.2180, -79.9050),
                    new GeoPoint(-2.2240, -79.8990),
                    new GeoPoint(-2.2150, -79.8860),
                    new GeoPoint(-2.2050, -79.8810),
                    new GeoPoint(-2.1950, -79.8840)
            ),
            1, List.of(
                    new GeoPoint(-2.1500, -79.9600),
                    new GeoPoint(-2.1580, -79.9680),
                    new GeoPoint(-2.1660, -79.9620),
                    new GeoPoint(-2.1740, -79.9700),
                    new GeoPoint(-2.1800, -79.9580),
                    new GeoPoint(-2.1700, -79.9480),
                    new GeoPoint(-2.1600, -79.9520)
            ),
            2, List.of(
                    new GeoPoint(-2.1200, -79.9200),
                    new GeoPoint(-2.1280, -79.9280),
                    new GeoPoint(-2.1360, -79.9240),
                    new GeoPoint(-2.1420, -79.9340),
                    new GeoPoint(-2.1480, -79.9260),
                    new GeoPoint(-2.1400, -79.9160),
                    new GeoPoint(-2.1300, -79.9140)
            ),
            3, List.of(
                    new GeoPoint(-2.2600, -79.9000),
                    new GeoPoint(-2.2680, -79.9080),
                    new GeoPoint(-2.2760, -79.9020),
                    new GeoPoint(-2.2840, -79.9100),
                    new GeoPoint(-2.2900, -79.9000)
            )
    );

    /**
     * Devuelve la ruta asociada al índice dado. Si el índice no tiene ruta propia,
     * retorna la primera como valor por defecto.
     */
    public List<GeoPoint> getRoute(int index) {
        return ROUTES.getOrDefault(index, ROUTES.get(0));
    }

    /** Número total de rutas disponibles. */
    public int routeCount() {
        return ROUTES.size();
    }
}
