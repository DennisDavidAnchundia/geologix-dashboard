import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import { apiFetch, getToken } from '../auth/api.js';

const WS_URL = import.meta.env.VITE_WS_URL ||
  `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws`;

export function useWebSocket(sessionKey = '') {
  const clientRef = useRef(null);
  const [connected, setConnected] = useState(false);
  const [vehicles, setVehicles] = useState({});
  const [alerts, setAlerts] = useState([]);

  const loadInitialAlerts = useCallback(() => {
    if (!getToken()) return; // aún sin login: no hay nada que cargar
    apiFetch('/api/alerts?activa=true')
      .then(res => res.json())
      .then(list => {
        if (Array.isArray(list) && list.length) {
          setAlerts(prev => {
            const existing = new Set(prev.map(a => a.id));
            const nuevos = list.filter(a => !existing.has(a.id));
            return [...nuevos, ...prev];
          });
        }
      })
      .catch(err => console.error('Error cargando alertas:', err));
  }, []);

  useEffect(() => {
    loadInitialAlerts();

    const client = new Client({
      brokerURL: WS_URL,
      connectHeaders: getToken() ? { Authorization: `Bearer ${getToken()}` } : {},
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        setConnected(true);

        client.subscribe('/topic/positions', (message) => {
          const position = JSON.parse(message.body);
          setVehicles(prev => ({
            ...prev,
            [position.vehicleId]: {
              ...prev[position.vehicleId],
              ...position
            }
          }));
        });

        client.subscribe('/topic/alerts', (message) => {
          const alert = JSON.parse(message.body);
          setAlerts(prev => [alert, ...prev].slice(0, 50));
        });
      },
      onDisconnect: () => {
        setConnected(false);
      },
      onStompError: (error) => {
        console.error('STOMP error:', error);
        setConnected(false);
      }
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, [sessionKey, loadInitialAlerts]);

  const clearAlerts = useCallback(() => {
    setAlerts([]);
  }, []);

  return { connected, vehicles, alerts, clearAlerts };
}
