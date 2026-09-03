import { useEffect, useState } from 'react';
import { Map } from './components/Map.jsx';
import { useWebSocket } from './hooks/useWebSocket.js';
import './App.css';

function App() {
  const { connected, vehicles, alerts } = useWebSocket();
  const [initialVehicles, setInitialVehicles] = useState([]);
  const [mostrandoAlerts, setMostrandoAlerts] = useState(true);

  useEffect(() => {
    const cargarFlota = async () => {
      try {
        const res = await fetch('/api/vehicles');
        const data = await res.json();
        setInitialVehicles(Array.isArray(data) ? data : []);
      } catch (e) {
        console.error('Error cargando flota:', e);
      }
    };
    cargarFlota();
  }, []);

  const vehicleCount = Object.keys(vehicles).length || initialVehicles.length;
  const activeCount = Object.values(vehicles).filter(v => v.ignition).length;
  const activeAlerts = alerts.filter(a => !a.resuelta);

  const promedioVelocidad = (() => {
    const values = Object.values(vehicles);
    if (!values.length) return 0;
    return Math.round(values.reduce((sum, v) => sum + (v.velocidadKmh || 0), 0) / values.length);
  })();

  const severityClass = (severidad) => {
    switch (severidad) {
      case 'ALTA': return 'severidad alta';
      case 'MEDIA': return 'severidad media';
      default: return 'severidad baja';
    }
  };

  const translations = {
    connectionState: connected ? ': Conexion en vivo' : ': Desconectado',
    vehicles: 'Vehiculos',
    active: 'Activos',
    averageSpeed: 'Vel. Promedio',
    alerts: 'Alertas',
    unit: 'km/h',
    alertTitle: 'Alertas en Tiempo Real',
    activeAlerts: 'Alertas activas',
    noAlerts: 'Sin alertas',
    fleetTitle: 'Flota de Vehiculos',
    vehicle: 'Camion',
    speed: 'Velocidad',
    status: 'Estado',
    ignition: 'Motor',
    on: 'Encendido',
    off: 'Apagado',
    lat: 'Lat',
    lon: 'Lon'
  };

  return (
    <div className="dashboard">
      <header className="header">
        <div className="logo">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M9 3v1m6-1v1M9 20v1m6-1v1M3 9h1m8-1v1m-7 1v1m12-1v1M3 15h1m12-1v1m7-1v1m-2-1h1M9 8h6v8H9z" />
          </svg>
          <span className="logo-text">GeoLogix</span>
          <span className="logo-sub">Dashboard GIS</span>
        </div>
        <div className="connection-status">
          <span className={`dot ${connected ? 'connected' : 'disconnected'}`} />
          <span className="status-text">{translations.connectionState}</span>
          <select
            className="alerts-filter"
            value={mostrandoAlerts ? 'activas' : 'todas'}
            onChange={(e) => setMostrandoAlerts(e.target.value === 'activas')}
          >
            <option value="activas">Solo activas</option>
            <option value="todas">Todas</option>
          </select>
        </div>
      </header>

      <div className="kpis">
        <div className="kpi">
          <span className="kpi-value">{vehicleCount}</span>
          <span className="kpi-label">{translations.vehicles}</span>
        </div>
        <div className="kpi">
          <span className="kpi-value green">{activeCount}</span>
          <span className="kpi-label">{translations.active}</span>
        </div>
        <div className="kpi">
          <span className="kpi-value">{promedioVelocidad}<small>{translations.unit}</small></span>
          <span className="kpi-label">{translations.averageSpeed}</span>
        </div>
        <div className="kpi danger">
          <span className="kpi-value red">{activeAlerts.length}</span>
          <span className="kpi-label">{translations.alerts}</span>
        </div>
      </div>

      <div className="main-area">
        <div className="map-container">
          <Map vehicles={vehicles} />
        </div>

        <aside className="side-panel">
          <section className="panel-section">
            <h2>{translations.alertTitle}</h2>
            {activeAlerts.length === 0 ? (
              <p className="empty-state">{translations.noAlerts}</p>
            ) : (
              <ul className="alerts-list">
                {activeAlerts.slice(0, 8).map(alert => (
                  <li key={alert.id} className={`alert-item ${severityClass(alert.severidad)}`}>
                    <div className="alert-header">
                      <span className="badge">{alert.severidad}</span>
                      <span className="alert-type">{alert.tipo.replace(/_/g, ' ')}</span>
                    </div>
                    <p className="alert-message">{alert.mensaje}</p>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="panel-section">
            <h2>{translations.fleetTitle}</h2>
            <ul className="fleet-list">
              {(initialVehicles.length ? initialVehicles : []).map(vehicle => {
                const lastPos = vehicles[vehicle.id] || {};
                const speed = lastPos.velocidadKmh ?? vehicle.ultimaPosicion?.velocidadKmh ?? 0;
                const lat = lastPos.latitude ?? vehicle.ultimaPosicion?.latitude;
                const lon = lastPos.longitude ?? vehicle.ultimaPosicion?.longitude;
                const estado = lastPos.ignition !== undefined ? (lastPos.ignition ? 'ACTIVO' : 'DETENIDO') : (vehicle.estado || 'ACTIVO');

                return (
                  <li key={vehicle.id} className="fleet-item">
                    <div className={`status-indicator ${estado === 'ACTIVO' ? 'active' : 'stopped'}`} />
                    <div className="fleet-info">
                      <span className="fleet-placa">{vehicle.placa}</span>
                      <span className="fleet-conductor">{vehicle.conductor}</span>
                      <span className="fleet-coords">
                        {lat?.toFixed(4)}, {lon?.toFixed(4)}
                      </span>
                    </div>
                    <div className="fleet-speed">
                      <span className="speed-value">{Math.round(speed)}</span>
                      <span className="speed-unit">{translations.unit}</span>
                    </div>
                  </li>
                );
              })}
            </ul>
          </section>
        </aside>
      </div>
    </div>
  );
}

export default App;
