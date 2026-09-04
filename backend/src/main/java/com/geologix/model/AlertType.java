package com.geologix.model;

/**
 * Tipos de alerta que puede generar el sistema de trazabilidad.
 */
public enum AlertType {
    /** El vehículo superó el límite de velocidad configurado. */
    EXCESO_VELOCIDAD,
    /** El vehículo permaneció detenido más tiempo del permitido. */
    DETENCION_PROLONGADA,
    /** El vehículo salió de una zona geográfica permitida (geofence). */
    FUERA_GEOFENCE,
    /** El vehículo ingresó a una zona geográfica (geofence). */
    INGRESO_ZONA,
    /** El vehículo salió de una zona de cobertura. */
    SALIDA_ZONA
}
