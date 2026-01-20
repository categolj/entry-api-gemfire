import React from 'react';

interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  helpText?: string;
}

export function Input({ label, error, helpText, className = '', ...props }: InputProps) {
  const inputId = props.id || `input-${Math.random().toString(36).substr(2, 9)}`;

  const inputClasses = `
    block w-full px-3 py-2 border placeholder-gray-400
    focus:outline-none focus:border-black transition-colors
    ${error ? 'border-red-400' : 'border-gray-300'}
    ${className}
  `.trim();

  return (
    <div>
      {label && (
        <label htmlFor={inputId} className="block text-sm font-medium text-black mb-1">
          {label}
        </label>
      )}
      <input id={inputId} className={inputClasses} {...props} />
      {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
      {helpText && !error && <p className="mt-1 text-sm text-gray-500">{helpText}</p>}
    </div>
  );
}