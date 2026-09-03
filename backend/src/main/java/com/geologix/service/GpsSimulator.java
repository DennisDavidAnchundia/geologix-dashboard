package com.geologix.service;

import com.geologix.config.SimulationProperties;
import com.geologix.model.GeoPoint;
import com.geologix.model.Position;
import com.geologix.model.Vehicle;
import com.geologix.model.VehicleStatus;
import com.geologix.repository.PositionRepository;
import com.geologix.repository.VehicleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Simula el comportamiento de un dispositivo GPS real.
 *
 * <p>Cada vehículo avanza por su ruta de reparto con una velocidad de crucero objetivo
 * (en km/h). Por cada tick se calcula la distancia que corresponde recorrer según esa
 * velocidad y se interpola dicha distancia a lo largo de la polilínea de la ruta, de modo
 * que el movimiento resulta fluido y realista (opuesto a saltar de punto a punto).
 *
 * <p>Para demostrar el motor de alertas, algunos vehículos simulan conductas de riesgo
 * (velocidad por encima del límite) y generan alertas de exceso de velocidad.
 */
@Service
public class GpsSimulator {

    private static final Logger log = LoggerFactory.getLogger(GpsSimulator.class);

    private final VehicleRepository vehicleRepository;
    private final PositionRepository positionRepository;
    private final RouteService routeService;
    private final AlertService alertService;
    private final RealtimePublisher publisher;
    private final SimulationProperties properties;

    /** Acumulado (km) recorrido por cada vehículo a lo largo de su ruta. */
    private final Map<Long, Double> progressKm = new HashMap<>();
    /** Velocidad de crucero objetivo (km/h) de cada vehículo. */
    private final Map<Long, Double> objetivoSpeeds = new HashMap<>();
    /** Siguiente tick en el que cada vehículo cambiará su velocidad objetivo. */
    private final Map<Long, Long> cambioVelocidadEn = new HashMap<>();

    public GpsSimulator(VehicleRepository vehicleRepository,
                        PositionRepository positionRepository,
                        RouteService routeService,
                        AlertService alertService,
                        RealtimePublisher publisher,
                        SimulationProperties properties) {
        this.vehicleRepository = vehicleRepository;
        this.positionRepository = positionRepository;
        this.routeService = routeService;
        this.alertService = alertService;
        this.publisher = publisher;
        this.properties = properties;
    }

    /**
     * Se ejecuta cada tick configurado: mueve todos los vehículos un paso por su ruta
     * a la velocidad objetivo y registra su nueva posición.
     */
    @Scheduled(fixedRateString = "${simulation.tick-interval-ms:2000}")
    public void tick() {
        for (Vehicle vehicle : vehicleRepository.findAll()) {
            double velocidad = objetivoSpeed(vehicle.getId());
            List<GeoPoint> route = routeService.getRoute(Math.toIntExact(vehicle.getId() % routeService.routeCount()));

            double routeLength = routeLength(route);
            double progress = progressKm.getOrDefault(vehicle.getId(), 0.0);
            double advanceKm = (velocidad / 3600.0) * (properties.getTickIntervalMs() / 1000.0);
            progress = (progress + advanceKm) % routeLength;
            progressKm.put(vehicle.getId(), progress);

            GeoPoint point = pointAt(route, routeLength, progress);
            boolean moving = velocidad > properties.getStoppedThresholdKmh();

            Position position = Position.builder()
                    .vehicle(vehicle)
                    .latitude(point.latitude())
                    .longitude(point.longitude())
                    .velocidadKmh(velocidad)
                    .ignition(moving)
                    .timestamp(Instant.now())
                    .build();
            positionRepository.save(position);
            publisher.publicarPosicion(position);

            VehicleStatus estado = moving ? VehicleStatus.ACTIVO : VehicleStatus.DETENIDO;
            if (vehicle.getEstado() != estado) {
                vehicle.setEstado(estado);
                vehicleRepository.save(vehicle);
            }

            alertService.evaluar(position);
        }
    }

    /**
     * Devuelve la velocidad objetivo del vehículo. La velocidad no es fija: cada
     * cierto tiempo el conductor "acelera" o "frena" (nueva velocidad objetivo),
     * lo que hace que los vehículos crucen el límite de velocidad de forma variable
     * y generen alertas NUEVAS de exceso de velocidad en tiempo real.
     */
    private double objetivoSpeed(Long vehicleId) {
        long now = System.currentTimeMillis();
        boolean tocaCambiar = now >= cambioVelocidadEn.getOrDefault(vehicleId, 0L);
        if (!objetivoSpeeds.containsKey(vehicleId) || tocaCambiar) {
            double velocidad = nuevaVelocidadObjetivo();
            objetivoSpeeds.put(vehicleId, velocidad);
            // Cambia la velocidad cada 15-30 s (demos nuevos datos y alertas).
            cambioVelocidadEn.put(vehicleId, now + 15_000
                    + ThreadLocalRandom.current().nextLong(0, 15_000));
        }
        return objetivoSpeeds.get(vehicleId);
    }

    /** Asigna una velocidad objetivo pseudoaleatoria dentro de un rango urbano realista. */
    private double nuevaVelocidadObjetivo() {
        double limite = properties.getSpeedLimitKmh();
        double velocidad;
        if (ThreadLocalRandom.current().nextBoolean()) {
            // Conductor prudente: entre 40 y el límite.
            velocidad = 40 + ThreadLocalRandom.current().nextDouble(0, Math.max(1, limite - 40));
        } else {
            // Conductor que excede el límite: hasta +25% por encima.
            velocidad = limite + ThreadLocalRandom.current().nextDouble(0, limite * 0.25);
        }
        return Math.round(velocidad * 100) / 100.0;
    }

    /** Longitud total (km) de una polilínea de ruta. */
    private double routeLength(List<GeoPoint> route) {
        double total = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            total += GeoDistance.distanceKm(route.get(i), route.get(i + 1));
        }
        return total;
    }

    /**
     * Devuelve el punto a {@code distanceKm} recorrida a lo largo de la polilínea,
     * interpolando dentro del segmento correspondiente.
     */
    private GeoPoint pointAt(List<GeoPoint> route, double totalLength, double distanceKm) {
        double remaining = Math.min(distanceKm, totalLength);
        for (int i = 0; i < route.size() - 1; i++) {
            GeoPoint a = route.get(i);
            GeoPoint b = route.get(i + 1);
            double segKm = GeoDistance.distanceKm(a, b);
            if (remaining <= segKm) {
                double t = segKm == 0 ? 0 : remaining / segKm;
                return new GeoPoint(
                        a.latitude() + (b.latitude() - a.latitude()) * t,
                        a.longitude() + (b.longitude() - a.longitude()) * t
                );
            }
            remaining -= segKm;
        }
        return route.get(route.size() - 1);
    }
}
