import React from 'react';

interface ErrorAlertProps {
  title?: string;
  message: string;
  onDismiss?: () => void;
}

export function ErrorAlert({ title = 'Something went wrong', message, onDismiss }: ErrorAlertProps) {
  return (
    <div
      role="alert"
      className="flex items-start gap-3 border border-redline/40 border-l-2 border-l-redline bg-redline-wash/60 px-4 py-3"
    >
      <span className="mt-0.5 font-mono text-[11px] uppercase tracking-wider text-redline">!</span>
      <div className="min-w-0 flex-1">
        <p className="font-sans text-sm font-semibold text-ink">{title}</p>
        <p className="mt-0.5 break-words text-sm leading-relaxed text-ink-soft">{message}</p>
      </div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          className="shrink-0 font-mono text-xs text-muted hover:text-ink"
        >
          Dismiss
        </button>
      )}
    </div>
  );
}
