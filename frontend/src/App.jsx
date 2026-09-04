import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import { Map } from './components/Map.jsx';
import { useWebSocket } from './hooks/useWebSocket.js';
import './App.css';

function App() {
  const { t, i18n } = useTranslation();
  const { connected, vehicles, alerts } = useWebSocket();
  const [initialVehicles, setInitialVehicles] = useState([]);
  const [selectedVehicleId, setSelectedVehicleId] = useState(null);
  const [mostrandoAlerts, setMostrandoAlerts] = useState(true);
  const [geofencesVisible, setGeofencesVisible] = useState(true);

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

  const promedioVelocidad = useMemo(() => {
    const values = Object.values(vehicles);
    if (!values.length) return 0;
    return Math.round(values.reduce((sum, v) => sum + (v.velocidadKmh || 0), 0) / values.length);
  }, [vehicles]);

  // Datos para el gráfico Recharts (velocidad por vehículo en vivo).
  const chartData = useMemo(() => {
    return Object.entries(vehicles)
      .map(([id, v]) => ({
        name: `V${id}`,
        velocidad: Math.round(v.velocidadKmh || 0)
      }))
      .slice(0, 8);
  }, [vehicles]);

  const severityClass = (severidad) => {
    switch (severidad) {
      case 'ALTA': return 'high';
      case 'MEDIA': return 'medium';
      default: return 'low';
    }
  };

  const toggleLang = () => {
    i18n.changeLanguage(i18n.language === 'es' ? 'en' : 'es');
  };

  const mapT = (key) => t(key);

  return (
    <div className="dashboard">
      <header className="header">
        <div className="logo">
          <svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M9 3v1m6-1v1M9 20v1m6-1v1M3 9h1m8-1v1m-7 1v1m12-1v1M3 15h1m12-1v1m7-1v1m-2-1h1M9 8h6v8H9z" />
          </svg>
          <span className="logo-text">{t('app.title')}</span>
          <span className="logo-sub">{t('app.subtitle')}</span>
        </div>
        <div className="connection-status">
          <span className={`dot ${connected ? 'connected' : 'disconnected'}`} />
          <span className="status-text">
            {connected ? `: ${t('connection.live')}` : `: ${t('connection.offline')}`}
          </span>
          <button className="lang-switch" onClick={toggleLang}>
            {i18n.language === 'es' ? 'EN' : 'ES'}
          </button>
          <button
            className={`geofence-toggle ${geofencesVisible ? 'active' : ''}`}
            onClick={() => setGeofencesVisible(v => !v)}
            title={t('map.geofences')}
          >
            {t('map.geofences')}
          </button>
          <select
            className="alerts-filter"
            value={mostrandoAlerts ? 'activas' : 'todas'}
            onChange={(e) => setMostrandoAlerts(e.target.value === 'activas')}
          >
            <option value="activas">{t('alerts.filterActive')}</option>
            <option value="todas">{t('alerts.filterAll')}</option>
          </select>
        </div>
      </header>

      <div className="kpis">
        <div className="kpi">
          <span className="kpi-value">{vehicleCount}</span>
          <span className="kpi-label">{t('kpi.vehicles')}</span>
        </div>
        <div className="kpi">
          <span className="kpi-value green">{activeCount}</span>
          <span className="kpi-label">{t('kpi.active')}</span>
        </div>
        <div className="kpi">
          <span className="kpi-value">{promedioVelocidad}<small>{t('kpi.unit')}</small></span>
          <span className="kpi-label">{t('kpi.avgSpeed')}</span>
        </div>
        <div className="kpi danger">
          <span className="kpi-value red">{activeAlerts.length}</span>
          <span className="kpi-label">{t('kpi.alerts')}</span>
        </div>
      </div>

      <div className="main-area">
        <div className="left-col">
          <div className="map-container">
            <Map
              vehicles={vehicles}
              selectedVehicleId={selectedVehicleId}
              onSelect={setSelectedVehicleId}
              t={mapT}
              geofencesVisible={geofencesVisible}
            />
          </div>

          <section className="panel-section chart-panel">
            <h2>{t('fleet.analytics')}</h2>
            <ResponsiveContainer width="100%" height={180}>
              <LineChart data={chartData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis dataKey="name" stroke="#94A3B8" />
                <YAxis stroke="#94A3B8" />
                <Tooltip
                  contentStyle={{ background: '#1E293B', border: '1px solid #334155', borderRadius: 8 }}
                  labelStyle={{ color: '#94A3B8' }}
                />
                <Line
                  type="monotone"
                  dataKey="velocidad"
                  name={t('kpi.avgSpeed')}
                  stroke="#38BDF8"
                  strokeWidth={2}
                  dot={{ r: 4 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </section>
        </div>

        <aside className="side-panel">
          <section className="panel-section">
            <h2>{t('alerts.title')}</h2>
            {activeAlerts.length === 0 ? (
              <p className="empty-state">{t('alerts.empty')}</p>
            ) : (
              <ul className="alerts-list">
                {activeAlerts.slice(0, 8).map(alert => (
                  <li key={alert.id} className={`alert-item ${severityClass(alert.severidad)}`}>
                    <div className="alert-header">
                      <span className="badge">{t(`alerts.severity.${alert.severidad}`)}</span>
                      <span className="alert-type">{alert.tipo.replace(/_/g, ' ')}</span>
                    </div>
                    <p className="alert-message">{alert.mensaje}</p>
                  </li>
                ))}
              </ul>
            )}
          </section>

          <section className="panel-section">
            <h2>{t('fleet.title')}</h2>
            <ul className="fleet-list">
              {(initialVehicles.length ? initialVehicles : []).map(vehicle => {
                const lastPos = vehicles[vehicle.id] || {};
                const speed = lastPos.velocidadKmh ?? vehicle.ultimaPosicion?.velocidadKmh ?? 0;
                const lat = lastPos.latitude ?? vehicle.ultimaPosicion?.latitude;
                const lon = lastPos.longitude ?? vehicle.ultimaPosicion?.longitude;
                const estado = lastPos.ignition !== undefined
                  ? (lastPos.ignition ? 'ACTIVO' : 'DETENIDO')
                  : (vehicle.estado || 'ACTIVO');
                const seleccionado = Number(selectedVehicleId) === Number(vehicle.id);

                return (
                  <li
                    key={vehicle.id}
                    className={`fleet-item ${seleccionado ? 'selected' : ''}`}
                    onClick={() => setSelectedVehicleId(seleccionado ? null : vehicle.id)}
                  >
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
                      <span className="speed-unit">{t('kpi.unit')}</span>
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
