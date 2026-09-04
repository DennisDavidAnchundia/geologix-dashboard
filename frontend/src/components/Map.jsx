import { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { GeofenceLayer } from './GeofenceLayer.jsx';

const VEHICLE_COLORS = {
  1: '#3B82F6',
  2: '#10B981',
  3: '#F59E0B',
  4: '#EF4444'
};

const createVehicleIcon = (color, speed) => {
  return L.divIcon({
    className: 'vehicle-marker',
    html: `
      <div style="
        width: 32px;
        height: 32px;
        background: ${color};
        border: 3px solid white;
        border-radius: 50%;
        box-shadow: 0 2px 6px rgba(0,0,0,0.3);
        display: flex;
        align-items: center;
        justify-content: center;
      ">
        <span style="color: white; font-size: 10px; font-weight: bold;">
          ${Math.round(speed)}
        </span>
      </div>
    `,
    iconSize: [32, 32],
    iconAnchor: [16, 16]
  });
};

// Clave pendiente para mantener la ruta de un sólo vehículo seleccionado.
let routeLine = null;

export function Map({ vehicles, selectedVehicleId, onSelect, t, geofencesVisible = true }) {
  const mapRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const markersRef = useRef({});

  useEffect(() => {
    if (mapInstanceRef.current) return;

    mapInstanceRef.current = L.map(mapRef.current, {
      center: [-2.1894, -79.8891],
      zoom: 13,
      zoomControl: true
    });

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19
    }).addTo(mapInstanceRef.current);

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    if (!mapInstanceRef.current) return;

    Object.entries(vehicles).forEach(([id, pos]) => {
      const color = VEHICLE_COLORS[id] || '#6B7280';
      const icon = createVehicleIcon(color, pos.velocidadKmh || 0);
      const desc = `${t('map.vehicleLabel')} ${id}`;

      if (markersRef.current[id]) {
        markersRef.current[id].setLatLng([pos.latitude, pos.longitude]);
        markersRef.current[id].setIcon(icon);
        markersRef.current[id].setPopupContent(
          `<strong>${desc}</strong><br/>${t('map.speed')}: ${Math.round(pos.velocidadKmh || 0)} km/h<br/>` +
          `${t('fleet.lat')}: ${pos.latitude?.toFixed(4)}<br/>` +
          `${t('fleet.lon')}: ${pos.longitude?.toFixed(4)}`
        );
      } else {
        const marker = L.marker([pos.latitude, pos.longitude], { icon })
          .addTo(mapInstanceRef.current)
          .bindPopup(
            `<strong>${desc}</strong><br/>${t('map.speed')}: ${Math.round(pos.velocidadKmh || 0)} km/h`
          );
        marker.on('click', () => onSelect && onSelect(id));
        markersRef.current[id] = marker;
      }
    });
  }, [vehicles, t, onSelect]);

  // Trazo de ruta del vehículo seleccionado.
  useEffect(() => {
    if (!mapInstanceRef.current || !selectedVehicleId) {
      if (routeLine) {
        routeLine.remove();
        routeLine = null;
      }
      return;
    }

    let cancelled = false;
    fetch(`/api/vehicles/${selectedVehicleId}/positions`)
      .then(res => res.json())
      .then(positions => {
        if (cancelled) return;
        const puntos = positions
          .map(p => [p.latitude, p.longitude])
          .filter(p => Number.isFinite(p[0]) && Number.isFinite(p[1]));

        if (routeLine) routeLine.remove();

        if (puntos.length > 1) {
          const color = VEHICLE_COLORS[selectedVehicleId] || '#3B82F6';
          routeLine = L.polyline(puntos, { color, weight: 3, opacity: 0.7 }).addTo(mapInstanceRef.current);
        } else if (puntos.length === 1) {
          mapInstanceRef.current.setView(puntos[0], 13);
        }
      })
      .catch(err => console.error('Error cargando ruta:', err));

    return () => { cancelled = true; };
  }, [selectedVehicleId]);

  return (
    <>
      <GeofenceLayer map={mapInstanceRef.current} visible={geofencesVisible} />
      <div ref={mapRef} style={{ width: '100%', height: '100%' }} />
    </>
  );
}
