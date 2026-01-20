import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Header } from '../../components/layout';
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
      navigate(state.from.pathname, { replace: true });
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
      setAuthError('Please enter both username and password');
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
      navigate(`/console/${tenantId}`);
    } catch (error) {
      // Reset redirect flag
      setSkipAuthRedirect(false);
      
      // If authentication fails, show error and keep credentials in form
      setAuthError('Invalid username or password');
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

  return (
    <div className="min-h-screen bg-white">
      <Header />

      <div className="flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
        <div className="max-w-md w-full space-y-6">
          <div>
            <h2 className="text-xl font-bold text-black">
              ## Select Tenant
            </h2>
            <p className="mt-2 text-sm text-gray-500">
              Choose a tenant to manage entries
            </p>
          </div>

          {/* Authentication Section */}
          <div className="border border-gray-200 p-6">
            <h3 className="text-base font-medium text-black mb-4">Authentication</h3>
            <div className="space-y-4">
              <div>
                <label htmlFor="username" className="block text-sm font-medium text-black mb-1">
                  Username
                </label>
                <input
                  id="username"
                  type="text"
                  value={username}
                  onChange={(e) => {
                    setUsername(e.target.value);
                    setAuthError('');
                  }}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' && username.trim() && password.trim() && !isAuthenticating) {
                      e.preventDefault();
                      handleDefaultTenant();
                    }
                  }}
                  placeholder="Enter username"
                  className="appearance-none block w-full px-3 py-2 border border-gray-300 placeholder-gray-400 text-black focus:outline-none focus:border-black transition-colors"
                />
              </div>
              <div>
                <label htmlFor="password" className="block text-sm font-medium text-black mb-1">
                  Password
                </label>
                <div className="relative">
                  <input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => {
                      setPassword(e.target.value);
                      setAuthError('');
                    }}
                    onKeyDown={(e) => {
                      if (e.key === 'Enter' && username.trim() && password.trim() && !isAuthenticating) {
                        e.preventDefault();
                        handleDefaultTenant();
                      }
                    }}
                    placeholder="Enter password"
                    className="appearance-none block w-full px-3 py-2 pr-10 border border-gray-300 placeholder-gray-400 text-black focus:outline-none focus:border-black transition-colors"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 flex items-center pr-3 text-gray-400 hover:text-black"
                  >
                    {showPassword ? (
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M3.98 8.223A10.477 10.477 0 0 0 1.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.451 10.451 0 0 1 12 4.5c4.756 0 8.773 3.162 10.065 7.498a10.522 10.522 0 0 1-4.293 5.774M6.228 6.228 3 3m3.228 3.228 3.65 3.65m7.894 7.894L21 21m-3.228-3.228-3.65-3.65m0 0a3 3 0 1 0-4.243-4.243m4.242 4.242L9.88 9.88" />
                      </svg>
                    ) : (
                      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="w-5 h-5">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M2.036 12.322a1.012 1.012 0 0 1 0-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178Z" />
                        <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
                      </svg>
                    )}
                  </button>
                </div>
              </div>
              {authError && (
                <p className="text-sm text-red-600">{authError}</p>
              )}
            </div>
          </div>

          <div className="space-y-4">
            {/* Default Tenant */}
            <button
              onClick={handleDefaultTenant}
              disabled={!username.trim() || !password.trim() || isAuthenticating}
              className="w-full text-left p-4 border border-gray-300 hover:border-black focus:outline-none transition-colors disabled:opacity-40 disabled:cursor-not-allowed disabled:hover:border-gray-300"
            >
              <div className="font-medium text-black">
                {isAuthenticating ? 'Authenticating...' : 'Default Tenant'}
              </div>
              <div className="text-sm text-gray-500 mt-1">Use the default tenant for general entries</div>
              <div className="text-xs text-gray-400 mt-1">Tenant ID: {DEFAULT_TENANT}</div>
            </button>

            {/* OR Divider */}
            <div className="relative">
              <div className="absolute inset-0 flex items-center">
                <div className="w-full border-t border-gray-200" />
              </div>
              <div className="relative flex justify-center text-sm">
                <span className="px-3 bg-white text-gray-400">OR</span>
              </div>
            </div>

            {/* Custom Tenant */}
            <div className="space-y-3">
              <h3 className="text-base font-medium text-black">Custom Tenant</h3>
              <form onSubmit={handleCustomTenantSubmit} className="space-y-3">
                <div>
                  <label htmlFor="custom-tenant" className="block text-sm font-medium text-black mb-1">
                    Tenant ID
                  </label>
                  <input
                    id="custom-tenant"
                    type="text"
                    value={customTenant}
                    onChange={(e) => setCustomTenant(e.target.value)}
                    placeholder="Enter tenant ID (e.g., blog, docs, etc.)"
                    className="appearance-none block w-full px-3 py-2 border border-gray-300 placeholder-gray-400 text-black focus:outline-none focus:border-black transition-colors"
                  />
                </div>
                <button
                  type="submit"
                  disabled={!customTenant.trim() || !username.trim() || !password.trim() || isAuthenticating}
                  className="w-full py-2 px-4 text-sm font-medium text-white bg-black hover:opacity-80 focus:outline-none disabled:opacity-40 disabled:cursor-not-allowed transition-opacity"
                >
                  {isAuthenticating ? 'Authenticating...' : 'Access Custom Tenant'}
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}