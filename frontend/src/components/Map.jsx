import { useEffect, useRef } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

const VEHICLE_COLORS = {
  1: '#3B82F6',
  2: '#10B981',
  3: '#F59E0B',
  4: '#EF4444'
};

const createVehicleIcon = (color, speed) => {
  const rotation = 0;
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
        transform: rotate(${rotation}deg);
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

export function Map({ vehicles }) {
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

      if (markersRef.current[id]) {
        markersRef.current[id].setLatLng([pos.latitude, pos.longitude]);
        markersRef.current[id].setIcon(icon);
        markersRef.current[id].setPopupContent(`
          <strong>Vehiculo ${id}</strong><br/>
          Velocidad: ${Math.round(pos.velocidadKmh || 0)} km/h<br/>
          Lat: ${pos.latitude?.toFixed(4)}<br/>
          Lon: ${pos.longitude?.toFixed(4)}
        `);
      } else {
        const marker = L.marker([pos.latitude, pos.longitude], { icon })
          .addTo(mapInstanceRef.current)
          .bindPopup(`
            <strong>Vehiculo ${id}</strong><br/>
            Velocidad: ${Math.round(pos.velocidadKmh || 0)} km/h
          `);
        markersRef.current[id] = marker;
      }
    });
  }, [vehicles]);

  return <div ref={mapRef} style={{ width: '100%', height: '100%' }} />;
}
