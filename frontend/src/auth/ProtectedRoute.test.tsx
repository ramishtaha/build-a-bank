// frontend/src/auth/ProtectedRoute.test.tsx
// Step 29 · route-guard test. Step 32 · the guard is session-aware: while the silent-refresh bootstrap is
// in flight it HOLDS (placeholder, no redirect); a settled anonymous redirects to /login; an established
// session renders the protected content. We drive the bootstrap by mocking the api module.
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import * as api from '../api/client';
import { AuthProvider } from '../auth/AuthContext';
import { ProtectedRoute } from './ProtectedRoute';

vi.mock('../api/client');

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<p>Login page</p>} />
          <Route
            path="/secret"
            element={
              <ProtectedRoute>
                <p>Secret area</p>
              </ProtectedRoute>
            }
          />
        </Routes>
      </AuthProvider>
    </MemoryRouter>,
  );
}

describe('ProtectedRoute', () => {
  beforeEach(() => vi.resetAllMocks());

  it('holds with a placeholder while the session bootstrap is deciding', () => {
    vi.mocked(api.refreshAccessToken).mockReturnValue(new Promise(() => undefined)); // never settles
    renderAt('/secret');
    expect(screen.getByText(/checking your session/i)).toBeInTheDocument();
    expect(screen.queryByText('Login page')).not.toBeInTheDocument(); // no premature bounce
  });

  it('redirects to /login once the bootstrap settles anonymous (refresh 401)', async () => {
    vi.mocked(api.refreshAccessToken).mockResolvedValue(null); // no live session
    renderAt('/secret');
    expect(await screen.findByText('Login page')).toBeInTheDocument();
    expect(screen.queryByText('Secret area')).not.toBeInTheDocument();
  });

  it('renders the protected content when the silent refresh restores the session', async () => {
    vi.mocked(api.refreshAccessToken).mockResolvedValue('restored-jwt');
    vi.mocked(api.getCurrentUser).mockResolvedValue({ username: 'alice', roles: ['ROLE_USER'] });
    renderAt('/secret');
    expect(await screen.findByText('Secret area')).toBeInTheDocument();
  });
});
