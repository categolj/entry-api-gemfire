import React from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helpText?: string;
}

export function Input({ label, error, helpText, className = '', ...props }: InputProps) {
  const inputId = props.id || `input-${Math.random().toString(36).substr(2, 9)}`;
  const inputClasses = `field ${error ? 'field-invalid' : ''} ${className}`.trim();

  return (
    <div>
      {label && (
        <label htmlFor={inputId} className="label">
          {label}
        </label>
      )}
      <input id={inputId} className={inputClasses} aria-invalid={error ? true : undefined} {...props} />
      {error && <p className="mt-1 font-mono text-[11px] text-redline">{error}</p>}
      {helpText && !error && <p className="mt-1 font-mono text-[11px] text-muted">{helpText}</p>}
    </div>
  );
}
