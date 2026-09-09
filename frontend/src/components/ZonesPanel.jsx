import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { apiFetch } from '../auth/api.js';

const TIPOS = ['ALMACEN', 'ZONA_REPARTO', 'RESTRINGIDA', 'COBERTURA'];

/** Panel de administración de zonas (solo ADMIN): listar, dibujar, activar/desactivar, borrar. */
export function ZonesPanel({ drawMode, setDrawMode, draft, setDraft, onZonesChanged }) {
  const { t } = useTranslation();
  const [zonas, setZonas] = useState([]);
  const [nombre, setNombre] = useState('');
  const [tipo, setTipo] = useState('ZONA_REPARTO');
  const [color, setColor] = useState('#38BDF8');
  const [error, setError] = useState('');

  const cargar = () => {
    apiFetch('/api/geofences')
      .then(res => res.json())
      .then(data => { if (Array.isArray(data)) setZonas(data); })
      .catch(err => console.error('Error cargando zonas:', err));
  };

  useEffect(cargar, []);

  const guardarZona = async () => {
    setError('');
    if (draft.length < 3) {
      setError(t('zones.minPoints'));
      return;
    }
    if (!nombre.trim()) {
      setError(t('zones.nameRequired'));
      return;
    }
    const res = await apiFetch('/api/geofences', {
      method: 'POST',
      body: JSON.stringify({
        nombre: nombre.trim(),
        tipo,
        // Backend espera [lon, lat]; el mapa da [lat, lon].
        coordenadas: draft.map(([lat, lon]) => [lon, lat]),
        color,
        activa: true
      })
    });
    if (!res.ok) {
      const msg = await res.text();
      setError(msg || `Error ${res.status}`);
      return;
    }
    setNombre('');
    setDraft([]);
    setDrawMode(false);
    cargar();
    onZonesChanged?.();
  };

  const toggleActiva = async (z) => {
    await apiFetch(`/api/geofences/${z.id}/activa`, {
      method: 'PATCH',
      body: JSON.stringify({ activa: !z.activa })
    });
    cargar();
    onZonesChanged?.();
  };

  const borrar = async (z) => {
    if (!window.confirm(`${t('zones.deleteConfirm')}: ${z.nombre}?`)) return;
    await apiFetch(`/api/geofences/${z.id}`, { method: 'DELETE' });
    cargar();
    onZonesChanged?.();
  };

  const cancelarDibujo = () => {
    setDraft([]);
    setDrawMode(false);
    setError('');
  };

  const inputCls = 'border border-neutral-200 rounded px-2.5 py-2 text-sm text-black focus:outline-none focus:border-black bg-white';

  return (
    <section className="bg-white border border-neutral-200 rounded-md p-3.5">
      <h2 className="text-xs font-extrabold uppercase tracking-[0.12em] border-b-2 border-neutral-900 pb-2 mb-3">
        {t('zones.title')}
      </h2>

      {!drawMode ? (
        <button onClick={() => setDrawMode(true)} className="w-full bg-neutral-900 hover:bg-black text-white rounded py-2.5 text-sm font-semibold cursor-pointer">
          {t('zones.draw')}
        </button>
      ) : (
        <div className="flex flex-col gap-2.5 mb-1">
          <p className="text-xs text-[#b23a00]">{t('zones.drawHint', { n: draft.length })}</p>
          <label className="flex flex-col gap-1 text-[11px] font-bold uppercase tracking-[0.1em] text-neutral-500">
            {t('zones.name')}
            <input value={nombre} onChange={e => setNombre(e.target.value)} placeholder="Ej: Zona Reparto Este" className={inputCls} />
          </label>
          <div className="flex gap-2.5">
            <label className="flex-1 flex flex-col gap-1 text-[11px] font-bold uppercase tracking-[0.1em] text-neutral-500">
              {t('zones.type')}
              <select value={tipo} onChange={e => setTipo(e.target.value)} className={inputCls}>
                {TIPOS.map(tp => <option key={tp} value={tp}>{tp.replace(/_/g, ' ')}</option>)}
              </select>
            </label>
            <label className="flex flex-col gap-1 text-[11px] font-bold uppercase tracking-[0.1em] text-neutral-500">
              {t('zones.color')}
              <input type="color" value={color} onChange={e => setColor(e.target.value)} className="w-12 h-[38px] p-1 border border-neutral-200 rounded bg-white cursor-pointer" />
            </label>
          </div>
          {error && <p className="text-[13px] text-red-700">{error}</p>}
          <div className="flex gap-2.5">
            <button onClick={guardarZona} className="flex-1 bg-[#ff4d00] hover:bg-[#b23a00] text-white rounded py-2 text-sm font-semibold cursor-pointer">
              {t('zones.save')}
            </button>
            <button onClick={cancelarDibujo} className="flex-1 border border-neutral-300 rounded py-2 text-sm text-neutral-500 hover:text-black hover:border-black cursor-pointer">
              {t('zones.cancel')}
            </button>
          </div>
        </div>
      )}

      <ul className="flex flex-col mt-1">
        {zonas.map(z => (
          <li key={z.id} className={`flex items-center gap-2.5 px-0.5 py-2 border-b border-neutral-100 last:border-0 ${z.activa ? '' : 'opacity-50'}`}>
            <span className="w-2.5 h-2.5 rounded-full shrink-0" style={{ background: z.color || '#38BDF8' }} />
            <div className="flex-1 flex flex-col">
              <span className="text-[13px] font-semibold">{z.nombre}</span>
              <span className="text-[11px] text-neutral-500 uppercase tracking-wider">{z.tipo.replace(/_/g, ' ')}</span>
            </div>
            <button onClick={() => toggleActiva(z)} title={t('zones.toggle')} className="w-[26px] h-[26px] border border-neutral-200 rounded text-[13px] text-neutral-500 hover:text-black hover:border-black cursor-pointer">
              {z.activa ? '✓' : '○'}
            </button>
            <button onClick={() => borrar(z)} title={t('zones.delete')} className="w-[26px] h-[26px] border border-neutral-200 rounded text-[13px] text-neutral-500 hover:text-red-700 hover:border-red-700 cursor-pointer">
              ✕
            </button>
          </li>
        ))}
      </ul>
    </section>
  );
}
