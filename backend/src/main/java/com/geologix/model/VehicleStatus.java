package com.geologix.model;

/**
 * Estado operativo de un vehículo de la flota.
 */
public enum VehicleStatus {
    /** En movimiento. */
    ACTIVO,
    /** Detenido (sin avance relevante). */
    DETENIDO,
    /** Presenta alguna alerta activa. */
    ALERTA
}
