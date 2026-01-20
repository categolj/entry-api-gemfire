import React from 'react';
import { Link } from 'react-router-dom';

interface HeaderProps {
  tenant?: string;
  isDefaultTenant?: boolean;
  showTenantInfo?: boolean;
  showNavigation?: boolean;
}

export function Header({ tenant, isDefaultTenant, showTenantInfo = false, showNavigation = false }: HeaderProps) {
  return (
    <nav className="bg-white border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-14">
          <div className="flex items-center">
            <Link
              to="/"
              className="text-lg font-bold text-black tracking-wide hover:opacity-70 transition-opacity"
            >
              ENTRY.CONSOLE
            </Link>
          </div>
          <div className="flex items-center space-x-6">
            {showNavigation && tenant && (
              <Link
                to={`/console/${tenant}`}
                className="text-sm text-black hover:opacity-70 transition-opacity"
              >
                Entries
              </Link>
            )}
            {showNavigation && tenant && (
              <Link
                to={`/console/${tenant}/entries/new`}
                className="text-sm text-black hover:opacity-70 transition-opacity"
              >
                New
              </Link>
            )}
            {showTenantInfo && tenant && (
              <span className="text-sm text-gray-500">
                {isDefaultTenant ? 'Default' : tenant}
              </span>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}