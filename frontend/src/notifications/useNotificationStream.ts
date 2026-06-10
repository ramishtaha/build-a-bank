// frontend/src/notifications/useNotificationStream.ts
// Step 30 · live updates via Server-Sent Events. The notification service (Step 20) pushes `transfer` events
// over text/event-stream; the browser's EventSource holds one long-lived connection and the hook accumulates
// the latest few. We subscribe through the gateway (Step 30 route) so it's the same single origin as the REST
// calls. (SSE, not WebSocket: the bank's push is one-way server→client — the simpler, auto-reconnecting fit.)
import { useEffect, useState } from 'react';

export interface TransferNotification {
  eventId: string;
  transactionId: string;
  fromAccount: string;
  toAccount: string;
  amount: number;
  occurredAt: string;
  message: string;
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';
const STREAM_URL = `${BASE_URL}/notifications/api/notifications/stream`;
const MAX_KEPT = 20;

export function useNotificationStream(): { notifications: TransferNotification[]; connected: boolean } {
  const [notifications, setNotifications] = useState<TransferNotification[]>([]);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    const source = new EventSource(STREAM_URL);
    source.onopen = () => setConnected(true);
    source.onerror = () => setConnected(false);

    const onTransfer = (event: Event) => {
      const data = (event as MessageEvent<string>).data;
      const notification = JSON.parse(data) as TransferNotification;
      setNotifications((current) => [notification, ...current].slice(0, MAX_KEPT));
    };
    source.addEventListener('transfer', onTransfer);

    return () => {
      source.removeEventListener('transfer', onTransfer);
      source.close();
    };
  }, []);

  return { notifications, connected };
}
