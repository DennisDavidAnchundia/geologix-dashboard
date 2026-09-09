import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { login } from '../auth/api.js';

export function Login({ onLogin }) {
  const { t, i18n } = useTranslation();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('geologix123');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const submit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const data = await login(username.trim(), password);
      onLogin(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-dvh flex items-center justify-center bg-[#f4f3ef] px-4">
      <form onSubmit={submit} className="w-full max-w-[340px] bg-white border border-neutral-200 border-t-[6px] border-t-[#ff4d00] rounded p-8 flex flex-col gap-3.5">
        <div>
          <h1 className="text-3xl font-extrabold tracking-tight uppercase">GeoLogix</h1>
          <p className="text-xs text-neutral-500 uppercase tracking-[0.14em] mt-1">{t('app.subtitle')}</p>
        </div>
        <label className="flex flex-col gap-1.5 text-[11px] font-bold uppercase tracking-[0.1em] text-neutral-500">
          {t('auth.user')}
          <input
            value={username}
            onChange={e => setUsername(e.target.value)}
            autoComplete="username"
            className="border border-neutral-200 rounded px-2.5 py-2 text-sm font-normal normal-case tracking-normal text-black focus:outline-none focus:border-black"
          />
        </label>
        <label className="flex flex-col gap-1.5 text-[11px] font-bold uppercase tracking-[0.1em] text-neutral-500">
          {t('auth.password')}
          <input
            type="password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            autoComplete="current-password"
            className="border border-neutral-200 rounded px-2.5 py-2 text-sm font-normal normal-case tracking-normal text-black focus:outline-none focus:border-black"
          />
        </label>
        {error && <p className="text-[13px] text-red-700">{error}</p>}
        <button
          type="submit"
          disabled={loading}
          className="bg-[#ff4d00] hover:bg-[#b23a00] text-white rounded py-2.5 text-[15px] font-bold cursor-pointer disabled:opacity-60 mt-1"
        >
          {loading ? '…' : t('auth.login')}
        </button>
        <p className="text-xs text-neutral-400 leading-relaxed border-t border-neutral-200 pt-3">
          {t('auth.hint')}
        </p>
        <button
          type="button"
          onClick={() => i18n.changeLanguage(i18n.language === 'es' ? 'en' : 'es')}
          className="self-center bg-neutral-900 text-white rounded px-3 py-1.5 text-xs font-bold cursor-pointer"
        >
          {i18n.language === 'es' ? 'EN' : 'ES'}
        </button>
      </form>
    </div>
  );
}
