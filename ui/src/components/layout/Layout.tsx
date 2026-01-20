import React from 'react';
import { Outlet } from 'react-router-dom';
import { useTenant } from '../../hooks';
import { Header } from './Header';

interface LayoutProps {
  children?: React.ReactNode;
}

export function Layout({ children }: LayoutProps) {
  const { tenant, isDefaultTenant } = useTenant();

  return (
    <div className="min-h-screen bg-white">
      <Header
        tenant={tenant}
        isDefaultTenant={isDefaultTenant}
        showTenantInfo={true}
        showNavigation={true}
      />

      {/* Main content */}
      <main className="max-w-7xl mx-auto py-8 px-4 sm:px-6 lg:px-8">
        {children || <Outlet />}
      </main>
    </div>
  );
}