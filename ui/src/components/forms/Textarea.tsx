import React from 'react';

interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
  helpText?: string;
}

export function Textarea({ label, error, helpText, className = '', ...props }: TextareaProps) {
  const textareaId = props.id || `textarea-${Math.random().toString(36).substr(2, 9)}`;

  const textareaClasses = `
    block w-full px-3 py-2 border placeholder-gray-400
    focus:outline-none focus:border-black transition-colors
    ${error ? 'border-red-400' : 'border-gray-300'}
    ${className}
  `.trim();

  return (
    <div>
      {label && (
        <label htmlFor={textareaId} className="block text-sm font-medium text-black mb-1">
          {label}
        </label>
      )}
      <textarea id={textareaId} className={textareaClasses} {...props} />
      {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
      {helpText && !error && <p className="mt-1 text-sm text-gray-500">{helpText}</p>}
    </div>
  );
}