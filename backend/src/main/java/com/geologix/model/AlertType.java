package com.geologix.model;

/**
 * Tipos de alerta que puede generar el sistema de trazabilidad.
 */
public enum AlertType {
    /** El vehículo superó el límite de velocidad configurado. */
    EXCESO_VELOCIDAD,
    /** El vehículo permaneció detenido más tiempo del permitido. */
    DETENCION_PROLONGADA,
    /** El vehículo salió de una zona geográfica permitida (geofence) — Fase 5. */
    FUERA_GEOFENCE
}
