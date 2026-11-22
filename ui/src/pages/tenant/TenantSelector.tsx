import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks';
import { LocationState } from '../../types';
import { api, setSkipAuthRedirect } from '../../services/api';

const DEFAULT_TENANT = '_';

export function TenantSelector() {
  const [customTenant, setCustomTenant] = useState('');
  const { auth, login } = useAuth();
  const [username, setUsername] = useState(auth.username || '');
  const [password, setPassword] = useState(auth.password || '');
  const [showPassword, setShowPassword] = useState(false);
  const [authError, setAuthError] = useState('');
  const [isAuthenticating, setIsAuthenticating] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  // Initialize form fields from auth state on mount only
  useEffect(() => {
    if (auth.username) {
      setUsername(auth.username);
    }
    if (auth.password) {
      setPassword(auth.password);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // If already authenticated and coming from a protected route, redirect back
  useEffect(() => {
    const state = location.state as LocationState | undefined;
    if (auth.isAuthenticated && state?.from) {
      void navigate(state.from.pathname, { replace: true });
    }
  }, [auth.isAuthenticated, location.state, navigate]);

  // Check for authentication errors from API
  useEffect(() => {
    const storedError = sessionStorage.getItem('authError');
    if (storedError) {
      setAuthError(storedError);
      sessionStorage.removeItem('authError');
    }
  }, []);

  const handleTenantAccess = async (tenantId: string) => {
    if (!username.trim() || !password.trim()) {
      setAuthError('Enter both a username and a password.');
      return;
    }

    setIsAuthenticating(true);
    setAuthError('');

    try {
      // First, temporarily set auth state for testing
      await login(username, password);

      // Skip auto-redirect for authentication test
      setSkipAuthRedirect(true);

      // Test the authentication with a lightweight API call
      await api.getEntries(tenantId, { size: 1 });

      // If successful, reset redirect flag and navigate to the console
      setSkipAuthRedirect(false);
      await navigate(`/console/${tenantId}`);
    } catch (error) {
      // Reset redirect flag
      setSkipAuthRedirect(false);

      // If authentication fails, show error and keep credentials in form
      setAuthError('Those credentials were not accepted. Check them and try again.');
      console.error('Authentication failed:', error);
      // Don't call logout() to preserve the form values
    } finally {
      setIsAuthenticating(false);
    }
  };

  const handleDefaultTenant = () => {
    void handleTenantAccess(DEFAULT_TENANT);
  };

  const handleCustomTenantSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (customTenant.trim()) {
      void handleTenantAccess(customTenant.trim());
    }
  };

  const canSubmit = username.trim() && password.trim() && !isAuthenticating;

  return (
    <div className="min-h-screen bg-paper lg:grid lg:grid-cols-[1.05fr_0.95fr]">
      {/* Editorial panel */}
      <section className="relative hidden flex-col justify-between overflow-hidden bg-ink px-12 py-10 text-sheet lg:flex">
        <div>
          <p className="inline-block border-b-[3px] border-pencil-light pb-1 font-sans text-2xl font-extrabold tracking-tightest">
            Entry Desk
          </p>
        </div>

        <div className="relative max-w-lg">
          <p className="font-mono text-[11px] uppercase tracking-[0.16em] text-pencil-light">
            Console access
          </p>
          <h1 className="mt-4 font-sans text-5xl font-bold leading-[1.02] tracking-tightest">
            Every entry starts as a draft.
          </h1>
          <p className="mt-5 max-w-md text-base leading-relaxed text-sheet/70">
            Write in Markdown, proof with the AI assistant, review the changes line by line,
            and send the entry to the API from one desk.
          </p>
          <div className="mt-10 flex items-center gap-4">
            <span className="h-px w-16 bg-pencil-light" aria-hidden="true" />
            <span className="font-mono text-[11px] text-sheet/50">Draft, proof, publish</span>
          </div>
        </div>

        <div className="flex items-end justify-between font-mono text-[10px] text-sheet/40">
          <span>Entry API console</span>
          <span>№ 001</span>
        </div>
      </section>

      {/* Access form */}
      <main className="flex min-h-screen items-center justify-center px-5 py-10 sm:px-8">
        <div className="w-full max-w-md">
          <div className="lg:hidden">
            <p className="brand-mark">Entry Desk</p>
            <p className="mt-3 text-sm text-ink-soft">Draft, proof, and publish entries.</p>
          </div>

          <div className="mt-8 lg:mt-0">
            <h2 className="font-sans text-3xl font-bold tracking-tightest">Open the desk</h2>
            <p className="mt-2 text-sm leading-relaxed text-ink-soft">
              Sign in with the credentials the entry API accepts.
            </p>
          </div>

          <form
            className="mt-6 space-y-4"
            onSubmit={(e) => {
              e.preventDefault();
              if (canSubmit) {
                handleDefaultTenant();
              }
            }}
          >
            <div>
              <label htmlFor="username" className="label">
                Username
              </label>
              <input
                id="username"
                type="text"
                autoComplete="username"
                value={username}
                onChange={(e) => {
                  setUsername(e.target.value);
                  setAuthError('');
                }}
                placeholder="Enter username"
                className="field"
              />
            </div>

            <div>
              <label htmlFor="password" className="label">
                Password
              </label>
              <div className="relative">
                <input
                  id="password"
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => {
                    setPassword(e.target.value);
                    setAuthError('');
                  }}
                  placeholder="Enter password"
                  className="field pr-16"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  aria-pressed={showPassword}
                  className="absolute inset-y-0 right-2 my-auto h-6 px-1 font-mono text-[10px] uppercase tracking-wider text-muted hover:text-ink"
                >
                  {showPassword ? 'Hide' : 'Show'}
                </button>
              </div>
            </div>

            {authError && (
              <p role="alert" className="border-l-2 border-redline bg-redline-wash/60 px-3 py-2 text-sm text-ink">
                {authError}
              </p>
            )}

            <button
              type="submit"
              disabled={!canSubmit}
              className="w-full rounded-[2px] bg-pencil px-4 py-2.5 font-sans text-sm font-semibold text-sheet transition-colors hover:bg-pencil-deep disabled:pointer-events-none disabled:opacity-40"
            >
              {isAuthenticating ? 'Authenticating…' : 'Open default tenant'}
            </button>
          </form>

          <div className="my-7 flex items-center gap-3">
            <span className="h-px flex-1 bg-rule" aria-hidden="true" />
            <span className="font-mono text-[10px] uppercase tracking-[0.14em] text-muted">
              Or another tenant
            </span>
            <span className="h-px flex-1 bg-rule" aria-hidden="true" />
          </div>

          <form onSubmit={handleCustomTenantSubmit} className="space-y-3">
            <div>
              <label htmlFor="custom-tenant" className="label">
                Tenant ID
              </label>
              <div className="flex gap-2">
                <input
                  id="custom-tenant"
                  type="text"
                  value={customTenant}
                  onChange={(e) => setCustomTenant(e.target.value)}
                  placeholder="e.g. blog, docs"
                  className="field"
                />
                <button
                  type="submit"
                  disabled={!customTenant.trim() || !canSubmit}
                  className="shrink-0 rounded-[2px] border border-ink/70 px-4 font-sans text-sm font-semibold text-ink transition-colors hover:bg-ink hover:text-sheet disabled:pointer-events-none disabled:opacity-40"
                >
                  Open tenant
                </button>
              </div>
            </div>
            <p className="font-mono text-[11px] text-muted">
              The default tenant is the shared blog. A custom tenant keeps a separate set of entries.
            </p>
          </form>
        </div>
      </main>
    </div>
  );
}
