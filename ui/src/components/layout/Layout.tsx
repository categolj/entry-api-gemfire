import React from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { useTenant } from '../../hooks';
import { Header } from './Header';

interface LayoutProps {
  children?: React.ReactNode;
}

const navLinkClass = ({ isActive }: { isActive: boolean }) =>
  [
    'group relative block border-b border-rule py-2.5 font-sans text-[15px] transition-colors',
    isActive ? 'font-semibold text-ink' : 'text-ink-soft hover:text-ink',
  ].join(' ');

export function Layout({ children }: LayoutProps) {
  const { tenant, isDefaultTenant } = useTenant();
  const tenantLabel = isDefaultTenant ? 'Default tenant' : tenant;

  return (
    <div className="min-h-screen bg-paper text-ink">
      <a
        href="#main"
        className="sr-only focus:not-sr-only focus:fixed focus:left-3 focus:top-3 focus:z-50 focus:bg-ink focus:px-3 focus:py-2 focus:text-sm focus:text-sheet"
      >
        Skip to content
      </a>

      <Header tenant={tenant} isDefaultTenant={isDefaultTenant} />

      <aside className="fixed inset-y-0 left-0 z-30 hidden w-60 flex-col border-r border-rule bg-paper px-6 py-7 lg:flex">
        <NavLink to="/" className="brand-mark">
          Entry Desk
        </NavLink>

        <div className="mt-7 border border-rule bg-sheet px-3 py-2.5">
          <p className="font-mono text-[11px] text-muted">Tenant</p>
          <p className="mt-1 truncate font-sans text-sm font-semibold text-ink">{tenantLabel}</p>
          <p className="mt-0.5 font-mono text-[11px] text-muted">
            {isDefaultTenant ? 'ID _' : 'Custom tenant'}
          </p>
        </div>

        <nav className="mt-8 flex flex-col" aria-label="Console">
          <NavLink to={`/console/${tenant}`} end className={navLinkClass}>
            Entries
          </NavLink>
          <NavLink to={`/console/${tenant}/entries/new`} className={navLinkClass}>
            New entry
          </NavLink>
        </nav>

        <div className="mt-auto">
          <NavLink
            to="/"
            className="font-sans text-xs font-semibold text-ink-soft underline decoration-rule underline-offset-4 hover:text-pencil hover:decoration-pencil"
          >
            Switch tenant
          </NavLink>
        </div>
      </aside>

      <div className="lg:pl-60">
        <main id="main" className="mx-auto w-full max-w-6xl px-5 py-8 sm:px-8 lg:py-12">
          {children || <Outlet />}
        </main>
      </div>
    </div>
  );
}
