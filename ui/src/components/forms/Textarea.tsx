import React from 'react';

interface TextareaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
  label?: string;
  error?: string;
  helpText?: string;
}

export function Textarea({ label, error, helpText, className = '', ...props }: TextareaProps) {
  const textareaId = props.id || `textarea-${Math.random().toString(36).substr(2, 9)}`;
  const textareaClasses = `field resize-y leading-relaxed ${error ? 'field-invalid' : ''} ${className}`.trim();

  return (
    <div>
      {label && (
        <label htmlFor={textareaId} className="label">
          {label}
        </label>
      )}
      <textarea id={textareaId} className={textareaClasses} aria-invalid={error ? true : undefined} {...props} />
      {error && <p className="mt-1 font-mono text-[11px] text-redline">{error}</p>}
      {helpText && !error && <p className="mt-1 font-mono text-[11px] text-muted">{helpText}</p>}
    </div>
  );
}
