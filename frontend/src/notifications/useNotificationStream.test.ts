// frontend/src/notifications/useNotificationStream.test.ts
// Step 30 · drive the SSE hook with a controllable EventSource (jsdom has none) — emit a `transfer` event and
// assert the hook accumulates it and reports connected.
import { act, renderHook, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { useNotificationStream } from './useNotificationStream';

class ControllableEventSource {
  static instances: ControllableEventSource[] = [];
  url: string;
  onopen: (() => void) | null = null;
  onerror: (() => void) | null = null;
  private readonly listeners = new Map<string, ((event: Event) => void)[]>();

  constructor(url: string) {
    this.url = url;
    ControllableEventSource.instances.push(this);
  }

  addEventListener(type: string, callback: (event: Event) => void): void {
    this.listeners.set(type, [...(this.listeners.get(type) ?? []), callback]);
  }

  removeEventListener(type: string, callback: (event: Event) => void): void {
    this.listeners.set(type, (this.listeners.get(type) ?? []).filter((listener) => listener !== callback));
  }

  close(): void {}

  emit(type: string, data: unknown): void {
    for (const callback of this.listeners.get(type) ?? []) {
      callback({ data: JSON.stringify(data) } as MessageEvent);
    }
  }
}

describe('useNotificationStream', () => {
  afterEach(() => {
    ControllableEventSource.instances = [];
    vi.unstubAllGlobals();
  });

  it('accumulates transfer events pushed over SSE and reports connected', async () => {
    vi.stubGlobal('EventSource', ControllableEventSource);
    const { result } = renderHook(() => useNotificationStream());

    expect(result.current.notifications).toHaveLength(0);

    act(() => {
      const source = ControllableEventSource.instances[0];
      source.onopen?.();
      source.emit('transfer', {
        eventId: 'e1',
        transactionId: 't1',
        fromAccount: 'ACC-A',
        toAccount: 'ACC-B',
        amount: 50,
        occurredAt: '2026-06-10T00:00:00Z',
        message: 'Transfer of 50 from ACC-A to ACC-B completed.',
      });
    });

    await waitFor(() => expect(result.current.notifications).toHaveLength(1));
    expect(result.current.notifications[0].message).toContain('ACC-A to ACC-B');
    expect(result.current.connected).toBe(true);
  });
});
