// frontend/src/notifications/LiveNotifications.tsx
// Step 30 · renders the live SSE feed. A connection dot + the latest transfer messages as they stream in.
import { useNotificationStream } from './useNotificationStream';

export function LiveNotifications() {
  const { notifications, connected } = useNotificationStream();

  return (
    <section aria-label="Live notifications">
      <h3>
        Live notifications <span aria-label={connected ? 'connected' : 'disconnected'}>{connected ? '🟢' : '⚪'}</span>
      </h3>
      {notifications.length === 0 ? (
        <p>Waiting for activity…</p>
      ) : (
        <ul>
          {notifications.map((notification) => (
            <li key={notification.eventId}>{notification.message}</li>
          ))}
        </ul>
      )}
    </section>
  );
}
