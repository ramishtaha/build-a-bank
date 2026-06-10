// frontend/src/test/renderWithProviders.tsx
// Step 30 · test helper — wraps a component in the same providers as the app (a fresh retry-off QueryClient,
// a MemoryRouter, and the AuthProvider) so query/mutation hooks and useAuth work under test.
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import type { ReactElement, ReactNode } from 'react';

import { AuthProvider } from '../auth/AuthContext';

export function renderWithProviders(ui: ReactElement) {
  // retry:false so error states surface immediately (no exponential backoff in tests)
  const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AuthProvider>{children}</AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>
    );
  }

  return render(ui, { wrapper: Wrapper });
}
