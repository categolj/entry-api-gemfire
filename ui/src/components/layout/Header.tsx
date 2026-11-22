import React from 'react';
import { NavLink } from 'react-router-dom';

interface HeaderProps {
  tenant?: string;
  isDefaultTenant?: boolean;
}

/**
 * Compact top bar shown on small screens, where the side rail is not available.
 */
export function Header({ tenant, isDefaultTenant }: HeaderProps) {
  const tenantLabel = isDefaultTenant ? 'Default tenant' : tenant;
  const tenantId = tenant ?? '_';

  return (
    <header className="sticky top-0 z-40 border-b border-rule bg-paper/95 backdrop-blur lg:hidden">
      <div className="flex h-14 items-center justify-between px-5">
        <NavLink to="/" className="brand-mark text-lg">
          Entry Desk
        </NavLink>
        <span className="font-mono text-[11px] text-muted">{tenantLabel}</span>
      </div>
      {tenant && (
        <nav className="flex items-center gap-6 border-t border-rule px-5" aria-label="Console">
          <NavLink
            to={`/console/${tenant}`}
            end
            className={({ isActive }) =>
              `border-b-2 py-2 font-sans text-sm ${isActive ? 'border-pencil font-semibold text-ink' : 'border-transparent text-ink-soft'}`
            }
          >
            Entries
          </NavLink>
          <NavLink
            to={`/console/${tenant}/entries/new`}
            className={({ isActive }) =>
              `border-b-2 py-2 font-sans text-sm ${isActive ? 'border-pencil font-semibold text-ink' : 'border-transparent text-ink-soft'}`
            }
          >
            New entry
          </NavLink>
          <NavLink
            to="/"
            className="ml-auto border-b-2 border-transparent py-2 font-mono text-[11px] text-muted"
            aria-label={`Switch tenant (current tenant id ${tenantId})`}
          >
            Switch
          </NavLink>
        </nav>
      )}
    </header>
  );
}
