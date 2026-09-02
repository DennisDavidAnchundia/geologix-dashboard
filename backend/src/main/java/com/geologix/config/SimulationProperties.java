package com.geologix.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parámetros configurados de la simulación GPS.
 * Se definen en application.properties bajo el prefijo "simulation".
 */
@ConfigurationProperties(prefix = "simulation")
public class SimulationProperties {

    /** Número de vehículos de la flota a simular. */
    private int vehicleCount = 4;

    /** Intervalo (ms) entre actualizaciones de posición de cada vehículo. */
    private long tickIntervalMs = 2000;

    /** Límite de velocidad (km/h) a partir del cual se genera una alerta. */
    private double speedLimitKmh = 80;

    /** Tiempo (ms) de detención que dispara una alerta de detención prolongada. */
    private long stopThresholdMs = 60_000;

    /** Velocidad (km/h) por debajo de la cual se considera al vehículo detenido. */
    private double stoppedThresholdKmh = 2;

    // Getters y setters
    public int getVehicleCount() { return vehicleCount; }
    public void setVehicleCount(int vehicleCount) { this.vehicleCount = vehicleCount; }

    public long getTickIntervalMs() { return tickIntervalMs; }
    public void setTickIntervalMs(long tickIntervalMs) { this.tickIntervalMs = tickIntervalMs; }

    public double getSpeedLimitKmh() { return speedLimitKmh; }
    public void setSpeedLimitKmh(double speedLimitKmh) { this.speedLimitKmh = speedLimitKmh; }

    public long getStopThresholdMs() { return stopThresholdMs; }
    public void setStopThresholdMs(long stopThresholdMs) { this.stopThresholdMs = stopThresholdMs; }

    public double getStoppedThresholdKmh() { return stoppedThresholdKmh; }
    public void setStoppedThresholdKmh(double stoppedThresholdKmh) { this.stoppedThresholdKmh = stoppedThresholdKmh; }
}
