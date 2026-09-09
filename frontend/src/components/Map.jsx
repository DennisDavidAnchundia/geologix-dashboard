import { useEffect, useRef, useState } from 'react';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { GeofenceLayer } from './GeofenceLayer.jsx';
import { apiFetch } from '../auth/api.js';

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
        width: 30px;
        height: 30px;
        background: ${color};
        border: 2px solid #ffffff;
        border-radius: 50%;
        box-shadow: 0 1px 4px rgba(0,0,0,0.25);
        display: flex;
        align-items: center;
        justify-content: center;
      ">
        <span style="color: #ffffff; font-size: 10px; font-weight: 600;">
          ${Math.round(speed)}
        </span>
      </div>
    `,
    iconSize: [30, 30],
    iconAnchor: [15, 15]
  });
};

// Clave pendiente para mantener la ruta de un sólo vehículo seleccionado.
let routeLine = null;

export function Map({
  vehicles, selectedVehicleId, onSelect, t,
  geofencesVisible = true, zonesRefreshKey = 0,
  drawMode = false, draft = [], onDraftChange,
  snapshot = [], layoutKey = ''
}) {
  const mapRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const markersRef = useRef({});
  const draftLayerRef = useRef(null);
  const draftDotsRef = useRef([]);
  const onDraftRef = useRef(onDraftChange);
  onDraftRef.current = onDraftChange;
  const [mapReady, setMapReady] = useState(null);

  useEffect(() => {
    if (mapInstanceRef.current) return;

    mapInstanceRef.current = L.map(mapRef.current, {
      center: [-2.1894, -79.8891],
      zoom: 13,
      zoomControl: false
    });
    L.control.zoom({ position: 'bottomleft' }).addTo(mapInstanceRef.current);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19
    }).addTo(mapInstanceRef.current);

    setMapReady(mapInstanceRef.current);

    return () => {
      // Al destruir el mapa, los marcadores viejos quedan huérfanos:
      // se descartan para que se recreen en el mapa nuevo.
      markersRef.current = {};
      draftDotsRef.current = [];
      draftLayerRef.current = null;
      routeLine = null;
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
      setMapReady(null);
    };
  }, []);

  // Al abrir/cerrar el inspector cambia el tamaño visible: repintar tiles.
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map) return;
    const id = setTimeout(() => map.invalidateSize(), 120);
    return () => clearTimeout(id);
  }, [layoutKey, mapReady]);

  // Modo dibujo: cada click agrega un vértice al borrador.
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map) return;
    if (!drawMode) {
      map.getContainer().style.cursor = '';
      return;
    }
    map.getContainer().style.cursor = 'crosshair';
    const handler = (e) => {
      onDraftRef.current?.((prev) => [...prev, [e.latlng.lat, e.latlng.lng]]);
    };
    map.on('click', handler);
    return () => {
      map.off('click', handler);
      map.getContainer().style.cursor = '';
    };
  }, [drawMode, mapReady]);

  // Dibuja el polígono borrador.
  useEffect(() => {
    const map = mapInstanceRef.current;
    if (!map) return;
    if (draftLayerRef.current) {
      map.removeLayer(draftLayerRef.current);
      draftLayerRef.current = null;
    }
    draftDotsRef.current.forEach(d => { try { map.removeLayer(d); } catch { /* ya removido */ } });
    draftDotsRef.current = [];
    if (draft.length > 0) {
      if (draft.length >= 3) {
        draftLayerRef.current = L.polygon(draft, {
          color: '#38BDF8', fillColor: '#38BDF8', fillOpacity: 0.25, weight: 2, dashArray: '6, 4'
        }).addTo(map);
      } else {
        draftLayerRef.current = L.polyline(draft, { color: '#38BDF8', weight: 2, dashArray: '6, 4' }).addTo(map);
      }
      draft.forEach(p => {
        const dot = L.circleMarker(p, { radius: 4, color: '#38BDF8', fillOpacity: 1 }).addTo(map);
        draftDotsRef.current.push(dot);
      });
    }
  }, [draft, mapReady]);

  useEffect(() => {
    if (!mapInstanceRef.current) return;

    // Base REST (última posición conocida) + vivo WS encima: los carros se ven
    // aunque el socket aún no entregó su primer mensaje.
    const combinado = {};
    snapshot.forEach(v => {
      if (v?.ultimaPosicion && Number.isFinite(v.ultimaPosicion.latitude)) {
        combinado[v.id] = { ...v.ultimaPosicion, vehicleId: v.id };
      }
    });
    Object.entries(vehicles).forEach(([id, pos]) => {
      combinado[id] = pos;
    });

    Object.entries(combinado).forEach(([id, pos]) => {
      try {
        if (!Number.isFinite(pos.latitude) || !Number.isFinite(pos.longitude)) return;
        const map = mapInstanceRef.current;
        const color = VEHICLE_COLORS[id] || '#6B7280';
        const icon = createVehicleIcon(color, pos.velocidadKmh || 0);
        const desc = `${t('map.vehicleLabel')} ${id}`;
        const existing = markersRef.current[id];

        // Solo se reutiliza si vive en el mapa actual; si no, se recrea.
        if (existing && existing._map === map) {
          existing.setLatLng([pos.latitude, pos.longitude]);
          existing.setIcon(icon);
          existing.setPopupContent(
            `<strong>${desc}</strong><br/>${t('map.speed')}: ${Math.round(pos.velocidadKmh || 0)} km/h<br/>` +
            `${t('fleet.lat')}: ${pos.latitude?.toFixed(4)}<br/>` +
            `${t('fleet.lon')}: ${pos.longitude?.toFixed(4)}`
          );
        } else {
          if (existing) { try { existing.remove(); } catch { /* huérfano */ } }
          const marker = L.marker([pos.latitude, pos.longitude], { icon })
            .addTo(map)
            .bindPopup(
              `<strong>${desc}</strong><br/>${t('map.speed')}: ${Math.round(pos.velocidadKmh || 0)} km/h`
            );
          marker.on('click', () => onSelect && onSelect(id));
          markersRef.current[id] = marker;
        }
      } catch (e) {
        console.error('Error pintando marcador:', id, e);
      }
    });
  }, [vehicles, snapshot, t, onSelect]);

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
    apiFetch(`/api/vehicles/${selectedVehicleId}/positions`)
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
      <GeofenceLayer map={mapReady} visible={geofencesVisible} refreshKey={zonesRefreshKey} />
      <div ref={mapRef} style={{ width: '100%', height: '100%' }} />
    </>
  );
}
