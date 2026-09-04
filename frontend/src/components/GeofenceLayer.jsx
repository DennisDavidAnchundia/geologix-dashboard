import { useEffect, useRef, useState } from 'react';
import L from 'leaflet';

const ZONA_COLORS = {
  ALMACEN: '#10B981',
  ZONA_REPARTO: '#3B82F6',
  RESTRINGIDA: '#EF4444',
  COBERTURA: '#8B5CF6'
};

export function GeofenceLayer({ map, visible }) {
  const [geofences, setGeofences] = useState([]);
  const layerRef = useRef([]);

  useEffect(() => {
    fetch('/api/geofences')
      .then(res => res.json())
      .then(data => {
        if (Array.isArray(data)) {
          setGeofences(data);
        }
      })
      .catch(err => console.error('Error cargando geofences:', err));
  }, []);

  useEffect(() => {
    if (!map) return;

    // Limpiar capas anteriores
    layerRef.current.forEach(layer => map.removeLayer(layer));
    layerRef.current = [];

    if (!visible) return;

    geofences.forEach(gf => {
      if (!gf.coordenadas || gf.coordenadas.length < 3) return;

      const latlngs = gf.coordenadas.map(c => [c[1], c[0]]);
      const color = gf.color || ZONA_COLORS[gf.tipo] || '#38BDF8';

      const polygon = L.polygon(latlngs, {
        color: color,
        fillColor: color,
        fillOpacity: 0.15,
        weight: 2,
        dashArray: gf.tipo === 'RESTRINGIDA' ? '6, 4' : null
      }).addTo(map);

      polygon.bindPopup(`
        <div style="font-family: 'Segoe UI', sans-serif; min-width: 150px;">
          <strong style="font-size: 14px;">${gf.nombre}</strong><br/>
          <span style="color: #666; font-size: 12px;">${gf.tipo.replace(/_/g, ' ')}</span>
        </div>
      `);

      polygon.bindTooltip(gf.nombre, {
        permanent: false,
        direction: 'center',
        className: 'geofence-tooltip'
      });

      layerRef.current.push(polygon);
    });

    return () => {
      layerRef.current.forEach(layer => map.removeLayer(layer));
      layerRef.current = [];
    };
  }, [map, geofences, visible]);

  return null;
}
