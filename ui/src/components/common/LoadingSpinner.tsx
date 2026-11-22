import React from 'react';

interface LoadingSpinnerProps {
  size?: 'sm' | 'md' | 'lg';
  color?: 'ink' | 'muted' | 'sheet';
  label?: string;
}

export function LoadingSpinner({ size = 'md', color = 'ink', label }: LoadingSpinnerProps) {
  const sizeClasses = {
    sm: 'h-3.5 w-3.5 border',
    md: 'h-6 w-6 border-2',
    lg: 'h-9 w-9 border-2',
  };

  const colorClasses = {
    ink: 'border-ink/20 border-t-pencil',
    muted: 'border-rule border-t-muted',
    sheet: 'border-sheet/30 border-t-sheet',
  };

  return (
    <div className="flex items-center justify-center gap-3">
      <span
        role="status"
        aria-label={label || 'Loading'}
        className={`inline-block animate-spin rounded-full ${sizeClasses[size]} ${colorClasses[color]}`}
      />
      {label && <span className="meta-line">{label}</span>}
    </div>
  );
}
