import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { LineChart, Line, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from 'recharts';
import { Map } from './components/Map.jsx';
import { Login } from './components/Login.jsx';
import { ZonesPanel } from './components/ZonesPanel.jsx';
import { useWebSocket } from './hooks/useWebSocket.js';
import { apiFetch, clearSession, getUser } from './auth/api.js';

function RailBtn({ active, onClick, title, label, badge, pulse, children }) {
  return (
    <button
      onClick={onClick}
      title={title}
      className={`relative flex md:flex-col flex-row items-center gap-1 md:w-full px-3.5 md:px-0 py-2 text-[9px] md:text-[10px] font-semibold uppercase tracking-wider transition-colors cursor-pointer border-l-[3px] border-t-[3px] md:border-t-0 ${
        active
          ? 'text-white border-l-transparent border-t-[#ff4d00] md:border-t-transparent md:border-l-[#ff4d00] bg-white/5'
          : 'text-neutral-400 border-transparent hover:text-white'
      }`}
    >
      {children}
      <span>{label}</span>
      {badge !== 0 && badge !== '' && (
        <em className="absolute top-0.5 right-1 md:right-2.5 not-italic min-w-[17px] h-[17px] px-1 rounded-full bg-[#ff4d00] text-white text-[10px] font-extrabold flex items-center justify-center">
          {badge}
        </em>
      )}
      {pulse && (
        <span className="absolute top-1.5 right-1 md:right-3 w-2 h-2 rounded-full bg-red-400 animate-ping" />
      )}
    </button>
  );
}

function App() {
  const { t, i18n } = useTranslation();
  const [session, setSession] = useState(() => getUser());
  const [vista, setVista] = useState('flota');
  const { connected, vehicles, alerts, clearAlerts } = useWebSocket(session?.username || '');
  const [initialVehicles, setInitialVehicles] = useState([]);
  const [selectedVehicleId, setSelectedVehicleId] = useState(null);
  const [geofencesVisible, setGeofencesVisible] = useState(true);
  const [drawMode, setDrawMode] = useState(false);
  const [draft, setDraft] = useState([]);
  const [zonesRefreshKey, setZonesRefreshKey] = useState(0);
  const [tabAlertas, setTabAlertas] = useState('activas');
  const [historial, setHistorial] = useState([]);
  const [cargandoHist, setCargandoHist] = useState(false);
  const [resueltasSesion, setResueltasSesion] = useState(() => new Set());
  const [abierto, setAbierto] = useState(false);
  const [conteoActivas, setConteoActivas] = useState(0);
  const [noLeidas, setNoLeidas] = useState(0);
  const maxIdVisto = useRef(0);

  const refrescarConteo = useCallback(async () => {
    try {
      const res = await apiFetch('/api/alerts/count?activa=true');
      const data = await res.json();
      if (typeof data.count === 'number') setConteoActivas(data.count);
    } catch (e) {
      console.error('Error contando alertas:', e);
    }
  }, []);

  useEffect(() => {
    if (session) refrescarConteo();
  }, [session, alerts, refrescarConteo]);

  const esAdmin = session?.role === 'ADMIN';

  const irAVista = (v) => {
    if (v === 'alertas') setNoLeidas(0);
    if (vista === v) {
      setAbierto(a => !a); // tocar la vista activa libera toda la pantalla
    } else {
      setVista(v);
      setAbierto(true);
    }
  };

  // Marca no-leídas cuando llegan alertas nuevas sin estar mirándolas.
  useEffect(() => {
    const max = alerts.reduce((m, a) => Math.max(m, a.id || 0), 0);
    if (max > maxIdVisto.current) {
      if (maxIdVisto.current !== 0 && vista !== 'alertas') setNoLeidas(n => n + 1);
      maxIdVisto.current = max;
    }
  }, [alerts, vista]);

  useEffect(() => {
    if (!session) return;
    const cargarFlota = async () => {
      try {
        const res = await apiFetch('/api/vehicles');
        const data = await res.json();
        setInitialVehicles(Array.isArray(data) ? data : []);
      } catch (e) {
        console.error('Error cargando flota:', e);
      }
    };
    cargarFlota();
  }, [session]);

  const cargarHistorial = useCallback(async () => {
    setCargandoHist(true);
    try {
      const res = await apiFetch('/api/alerts?limit=100');
      const data = await res.json();
      setHistorial(Array.isArray(data) ? data : []);
    } catch (e) {
      console.error('Error cargando historial:', e);
    } finally {
      setCargandoHist(false);
    }
  }, []);

  useEffect(() => {
    if (session && vista === 'alertas' && tabAlertas === 'historial') {
      cargarHistorial();
    }
  }, [session, vista, tabAlertas, cargarHistorial]);

  const resolverTodas = async () => {
    try {
      const res = await apiFetch('/api/alerts/resolver-todas', { method: 'PATCH' });
      if (!res.ok) return;
      setResueltasSesion(prev => {
        const next = new Set(prev);
        alerts.forEach(a => next.add(a.id));
        return next;
      });
      refrescarConteo();
      cargarHistorial();
    } catch (e) {
      console.error('Error resolviendo todas:', e);
    }
  };

  const resolverAlerta = async (id) => {
    try {
      const res = await apiFetch(`/api/alerts/${id}/resolver`, { method: 'PATCH' });
      if (!res.ok) return;
      const actualizada = await res.json();
      setResueltasSesion(prev => new Set(prev).add(actualizada.id));
      setHistorial(prev => prev.map(a => (a.id === actualizada.id ? actualizada : a)));
      refrescarConteo();
    } catch (e) {
      console.error('Error resolviendo alerta:', e);
    }
  };

  const vehicleCount = Object.keys(vehicles).length || initialVehicles.length;
  const activeCount = Object.values(vehicles).filter(v => v.ignition).length;
  const activeAlerts = alerts.filter(a => !a.resuelta && !resueltasSesion.has(a.id));

  const promedioVelocidad = useMemo(() => {
    const values = Object.values(vehicles);
    if (!values.length) return 0;
    return Math.round(values.reduce((sum, v) => sum + (v.velocidadKmh || 0), 0) / values.length);
  }, [vehicles]);

  const chartData = useMemo(() => {
    return Object.entries(vehicles)
      .map(([id, v]) => ({
        name: `V${id}`,
        velocidad: Math.round(v.velocidadKmh || 0)
      }))
      .slice(0, 8);
  }, [vehicles]);

  const sevText = (s) => (s === 'ALTA' ? 'text-red-700' : s === 'MEDIA' ? 'text-amber-700' : 'text-green-700');
  const sevBar = (s) => (s === 'ALTA'
    ? 'shadow-[inset_3px_0_0_#dc2626]'
    : s === 'MEDIA'
      ? 'shadow-[inset_3px_0_0_#d97706]'
      : 'shadow-[inset_3px_0_0_#15803d]');

  const fmtHora = (ts) => {
    try {
      const d = new Date(ts);
      return `${d.toLocaleDateString()} ${d.toLocaleTimeString()}`;
    } catch {
      return '';
    }
  };

  const logout = () => {
    clearSession();
    clearAlerts();
    setSession(null);
  };

  const mapT = (key) => t(key);

  if (!session) {
    return <Login onLogin={(data) => setSession({ username: data.username, role: data.role })} />;
  }

  const listaAlertas = tabAlertas === 'activas' ? activeAlerts : historial;

  return (
    <div className="h-dvh flex flex-col md:grid md:grid-cols-[76px_1fr] bg-[#f4f3ef] text-neutral-900 overflow-hidden">
      {/* Riel: barra lateral en escritorio, dock flotante en móvil */}
      <aside className="rail order-2 md:order-1 z-[1100] flex md:flex-col flex-row items-stretch md:items-center justify-center gap-1 bg-transparent md:bg-neutral-950 px-3 pb-3 md:p-0 md:py-4 pointer-events-none md:pointer-events-auto">
        <div className="hidden md:flex w-10 h-10 rounded-md bg-[#ff4d00] text-white text-xl font-extrabold items-center justify-center mb-3">
          G
        </div>
        <nav className="pointer-events-auto flex md:flex-col flex-row items-center gap-0.5 md:gap-1.5 flex-1 md:flex-none md:w-full justify-center bg-neutral-950 rounded-full md:rounded-none px-3 md:px-0 py-1.5 md:py-0 shadow-[0_8px_28px_rgba(0,0,0,0.35)] md:shadow-none">
          <RailBtn active={vista === 'flota'} onClick={() => irAVista('flota')} title={t('nav.fleet')} label={t('nav.fleet')}>
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.8">
              <path d="M1 8h13v8H1zM14 11h4l3 3v2h-7z" />
              <circle cx="6" cy="18" r="1.8" /><circle cx="17" cy="18" r="1.8" />
            </svg>
          </RailBtn>
          <RailBtn active={vista === 'alertas'} onClick={() => irAVista('alertas')} title={t('nav.alerts')} label={t('nav.alerts')} badge={conteoActivas > 99 ? '99+' : conteoActivas} pulse={noLeidas > 0}>
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.8">
              <path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6" />
              <path d="M10 20a2 2 0 0 0 4 0" />
            </svg>
          </RailBtn>
          {esAdmin && (
            <RailBtn active={vista === 'zonas'} onClick={() => irAVista('zonas')} title={t('nav.zones')} label={t('nav.zones')}>
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.8">
                <path d="M4 4l7 3-2 7-7-3zM11 7l9-2-4 9-3-2z" />
              </svg>
            </RailBtn>
          )}
          <button
            onClick={() => setGeofencesVisible(v => !v)}
            title={t('map.geofences')}
            className="md:mt-2 md:w-full flex md:flex-col flex-row items-center gap-1 px-3 py-2 text-[10px] font-semibold uppercase tracking-wider text-neutral-400 hover:text-white cursor-pointer"
          >
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" strokeWidth="1.8">
              {geofencesVisible ? (
                <><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6z" /><circle cx="12" cy="12" r="2.5" /></>
              ) : (
                <><path d="M2 12s3.5-6 10-6 10 6 10 6-3.5 6-10 6-10-6-10-6z" /><path d="M4 4l16 16" /></>
              )}
            </svg>
            <span className="hidden md:inline">{t('map.geofences')}</span>
          </button>
        </nav>
        <div className="hidden md:flex md:flex-col flex-row items-center gap-2">
          <span
            title={connected ? t('connection.live') : t('connection.offline')}
            className={`w-2 h-2 rounded-full ${connected ? 'bg-green-400' : 'bg-red-500'}`}
          />
          <button
            onClick={() => i18n.changeLanguage(i18n.language === 'es' ? 'en' : 'es')}
            className="border border-neutral-700 rounded text-[11px] font-bold px-2 py-1 text-neutral-300 hover:text-white hover:border-white cursor-pointer"
          >
            {i18n.language === 'es' ? 'EN' : 'ES'}
          </button>
          <button
            onClick={logout}
            title={`${session.username} · ${session.role} — ${t('auth.logout')}`}
            className="w-[30px] h-[30px] rounded-full bg-neutral-800 text-[11px] font-bold text-white hover:bg-neutral-700 cursor-pointer"
          >
            {session.username.slice(0, 2).toUpperCase()}
          </button>
        </div>
      </aside>

      {/* Mapa a pantalla completa */}
      <main className="order-1 md:order-2 relative flex-1 min-h-0 min-w-0">
        <div className="absolute inset-0">
          <Map
            vehicles={vehicles}
            selectedVehicleId={selectedVehicleId}
            onSelect={setSelectedVehicleId}
            t={mapT}
            geofencesVisible={geofencesVisible}
            zonesRefreshKey={zonesRefreshKey}
            drawMode={drawMode}
            draft={draft}
            onDraftChange={setDraft}
            snapshot={initialVehicles}
            layoutKey={`${abierto}-${vista}`}
          />
        </div>

        <div className="absolute top-3 left-3 right-3 md:right-auto z-[500] flex gap-2">
          {[
            { v: vehicleCount, l: t('kpi.vehicles'), c: '', bar: 'border-t-neutral-900' },
            { v: activeCount, l: t('kpi.active'), c: 'text-green-700', bar: 'border-t-green-700' },
            { v: <>{promedioVelocidad}<small className="text-xs font-medium text-neutral-500"> {t('kpi.unit')}</small></>, l: t('kpi.avgSpeed'), c: '', bar: 'border-t-amber-600' },
            { v: conteoActivas, l: t('kpi.alerts'), c: 'text-red-600', bar: 'border-t-red-600' }
          ].map((k, i) => (
            <div key={i} className={`chip min-w-0 flex-1 md:flex-none md:min-w-[104px] bg-white/95 border border-neutral-200 border-t-[3px] ${k.bar} rounded-md px-2.5 md:px-3.5 py-1.5 shadow-sm flex flex-col`}>
              <b className={`text-xl md:text-2xl font-extrabold tracking-tight leading-tight ${k.c}`}>{k.v}</b>
              <span className="text-[10px] uppercase tracking-[0.1em] text-neutral-500 truncate">{k.l}</span>
            </div>
          ))}
        </div>

        {drawMode && (
          <div className="absolute z-[500] top-3 right-3 md:top-3 md:bottom-auto bottom-24 left-3 right-3 md:left-auto flex items-center gap-2.5 bg-neutral-900 text-white text-[13px] rounded-md pl-3.5 pr-2.5 py-2 shadow-lg">
            <span className="flex-1 md:flex-none">{t('zones.drawing', { n: draft.length })}</span>
            <button
              onClick={() => { setDraft([]); setDrawMode(false); }}
              className="border border-neutral-600 rounded px-3 py-1 text-xs hover:border-white cursor-pointer"
            >
              {t('zones.cancel')}
            </button>
          </div>
        )}
      </main>

      {/* Inspector flotante */}
      <aside
        className={`thin-scroll fixed z-[1000] bg-[#f4f3ef] flex flex-col gap-3 overflow-y-auto p-3 transition-transform duration-200
          inset-x-0 bottom-[68px] max-h-[54vh] rounded-t-xl border-t border-neutral-200 shadow-[0_-12px_32px_rgba(0,0,0,0.15)]
          md:inset-x-auto md:bottom-0 md:top-0 md:right-0 md:w-[364px] md:max-h-none md:rounded-none md:border-t-0 md:border-l md:shadow-[-12px_0_32px_rgba(0,0,0,0.12)]
          ${abierto ? 'translate-x-0 translate-y-0' : 'translate-x-0 md:translate-x-[105%] translate-y-[calc(100%+90px)] md:translate-y-0'}`}
      >
        <button
          onClick={() => setAbierto(false)}
          className="self-end w-[30px] h-[30px] shrink-0 border border-neutral-300 rounded bg-white text-sm text-neutral-500 hover:text-black hover:border-black cursor-pointer"
        >
          ✕
        </button>
        <div className="md:hidden flex items-center gap-2 bg-white border border-neutral-200 rounded-md px-3 py-2">
          <span className="w-8 h-8 rounded-full bg-neutral-900 text-white text-xs font-bold flex items-center justify-center">
            {session.username.slice(0, 2).toUpperCase()}
          </span>
          <span className="flex-1 flex flex-col">
            <b className="text-[13px]">{session.username}</b>
            <span className="text-[11px] text-neutral-500">{session.role}</span>
          </span>
          <button
            onClick={() => i18n.changeLanguage(i18n.language === 'es' ? 'en' : 'es')}
            className="border border-neutral-300 rounded text-[11px] font-bold px-2 py-1 cursor-pointer"
          >
            {i18n.language === 'es' ? 'EN' : 'ES'}
          </button>
          <button
            onClick={logout}
            className="border border-neutral-300 rounded text-[11px] font-bold px-2 py-1 text-red-700 cursor-pointer"
          >
            {t('auth.logout')}
          </button>
        </div>

        {vista === 'flota' && (
          <>
            <section className="bg-white border border-neutral-200 rounded-md p-3.5">
              <h2 className="text-xs font-extrabold uppercase tracking-[0.12em] border-b-2 border-neutral-900 pb-2 mb-1">
                {t('fleet.title')}
              </h2>
              <ul>
                {initialVehicles.map(vehicle => {
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
                      onClick={() => setSelectedVehicleId(seleccionado ? null : vehicle.id)}
                      className={`fleet-item flex items-center gap-3 px-2 py-2.5 border-b border-neutral-100 last:border-0 cursor-pointer hover:bg-neutral-50 ${seleccionado ? 'bg-orange-50 shadow-[inset_3px_0_0_#ff4d00]' : ''}`}
                    >
                      <span className={`w-2 h-2 rounded-full shrink-0 ${estado === 'ACTIVO' ? 'bg-green-700' : 'bg-amber-600'}`} />
                      <span className="flex-1 flex flex-col">
                        <span className="text-[15px] font-bold tracking-tight">{vehicle.placa}</span>
                        <span className="text-xs text-neutral-500">{vehicle.conductor}</span>
                        <span className="text-[11px] text-neutral-400">{lat?.toFixed(4)}, {lon?.toFixed(4)}</span>
                      </span>
                      <span className="flex items-baseline gap-1">
                        <span className="text-2xl font-extrabold tracking-tight">{Math.round(speed)}</span>
                        <span className="text-[11px] text-neutral-500">{t('kpi.unit')}</span>
                      </span>
                    </li>
                  );
                })}
              </ul>
            </section>
            <section className="bg-white border border-neutral-200 rounded-md p-3.5">
              <h2 className="text-xs font-extrabold uppercase tracking-[0.12em] border-b-2 border-neutral-900 pb-2 mb-2">
                {t('fleet.analytics')}
              </h2>
              <ResponsiveContainer width="100%" height={170}>
                <LineChart data={chartData}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e3e1dc" />
                  <XAxis dataKey="name" stroke="#a8adac" tick={{ fontSize: 11 }} tickLine={false} axisLine={{ stroke: '#e3e1dc' }} />
                  <YAxis stroke="#a8adac" tick={{ fontSize: 11 }} tickLine={false} axisLine={false} width={32} />
                  <Tooltip contentStyle={{ background: '#fff', border: '1px solid #141412', borderRadius: 4, fontSize: 12 }} />
                  <Line type="monotone" dataKey="velocidad" name={t('kpi.avgSpeed')} stroke="#141412" strokeWidth={2} dot={false} />
                </LineChart>
              </ResponsiveContainer>
            </section>
          </>
        )}

        {vista === 'alertas' && (
          <section className="bg-white border border-neutral-200 rounded-md p-3.5 flex-1 flex flex-col min-h-0">
            <h2 className="text-xs font-extrabold uppercase tracking-[0.12em] border-b-2 border-neutral-900 pb-2 mb-2.5">
              {t('alerts.title')}
            </h2>
            <div className="flex border border-neutral-200 rounded-md overflow-hidden mb-2">
              <button
                onClick={() => setTabAlertas('activas')}
                className={`flex-1 py-2 text-[13px] font-semibold cursor-pointer ${tabAlertas === 'activas' ? 'bg-neutral-900 text-white' : 'text-neutral-500 hover:text-black'}`}
              >
                {t('alerts.tabActive')} ({conteoActivas})
              </button>
              <button
                onClick={() => setTabAlertas('historial')}
                className={`flex-1 py-2 text-[13px] font-semibold cursor-pointer ${tabAlertas === 'historial' ? 'bg-neutral-900 text-white' : 'text-neutral-500 hover:text-black'}`}
              >
                {t('alerts.tabHistory')}
              </button>
              {tabAlertas === 'historial' && (
                <button onClick={cargarHistorial} title={t('alerts.refresh')} className="px-3 border-l border-neutral-200 text-base cursor-pointer hover:bg-neutral-100">
                  ↻
                </button>
              )}
            </div>
            {tabAlertas === 'activas' && listaAlertas.length > 0 && (
              <button onClick={resolverTodas} className="mb-2 w-full border border-green-700 text-green-700 rounded-md py-1.5 text-[13px] font-bold hover:bg-green-700 hover:text-white cursor-pointer">
                {t('alerts.resolveAll')}
              </button>
            )}
            {cargandoHist ? (
              <p className="text-center text-neutral-400 text-[13px] py-5">…</p>
            ) : listaAlertas.length === 0 ? (
              <p className="text-center text-neutral-400 text-[13px] py-5">{t('alerts.empty')}</p>
            ) : (
              <ul className="flex flex-col md:max-h-[calc(100dvh-260px)] md:overflow-y-auto thin-scroll">
                {listaAlertas.map(alert => (
                  <li key={alert.id} className={`py-2 pl-2.5 pr-1 border-b border-neutral-100 last:border-0 ${sevBar(alert.severidad)} ${alert.resuelta ? 'opacity-60' : ''}`}>
                    <div className="flex items-baseline gap-2 mb-0.5">
                      <span className={`text-[10px] font-extrabold uppercase tracking-wider ${sevText(alert.severidad)}`}>
                        {t(`alerts.severity.${alert.severidad}`)}
                      </span>
                      <span className="text-xs font-semibold">{alert.tipo.replace(/_/g, ' ')}</span>
                    </div>
                    <p className="text-xs text-neutral-500 leading-relaxed">{alert.mensaje}</p>
                    <div className="flex justify-between items-center mt-1.5 text-[11px] text-neutral-400">
                      <span>#{alert.id} · V{alert.vehicleId} · {fmtHora(alert.timestamp)}</span>
                      {!alert.resuelta ? (
                        <button onClick={() => resolverAlerta(alert.id)} className="border border-green-700 text-green-700 rounded text-[11px] font-bold px-2.5 py-0.5 hover:bg-green-700 hover:text-white cursor-pointer">
                          {t('alerts.resolve')}
                        </button>
                      ) : (
                        <span className="text-[11px] font-bold uppercase tracking-wide text-green-700">{t('alerts.resolved')}</span>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            )}
          </section>
        )}

        {vista === 'zonas' && esAdmin && (
          <ZonesPanel
            drawMode={drawMode}
            setDrawMode={setDrawMode}
            draft={draft}
            setDraft={setDraft}
            onZonesChanged={() => setZonesRefreshKey(k => k + 1)}
          />
        )}
      </aside>
    </div>
  );
}

export default App;
