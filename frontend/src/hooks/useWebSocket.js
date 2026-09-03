import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';

const WS_URL = `${window.location.protocol === 'https:' ? 'wss:' : 'ws:'}//${window.location.host}/ws`;

export function useWebSocket() {
  const clientRef = useRef(null);
  const [connected, setConnected] = useState(false);
  const [vehicles, setVehicles] = useState({});
  const [alerts, setAlerts] = useState([]);

  useEffect(() => {
    const client = new Client({
      brokerURL: WS_URL,
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
  }, []);

  const clearAlerts = useCallback(() => {
    setAlerts([]);
  }, []);

  return { connected, vehicles, alerts, clearAlerts };
}
