import React, { useState, useId } from 'react';

interface CategoryInputProps {
  label?: string;
  value: string[];
  onChange: (categories: string[]) => void;
  placeholder?: string;
  error?: string;
  disabled?: boolean;
}

export function CategoryInput({
  label,
  value,
  onChange,
  placeholder = 'e.g. Tech > Programming',
  error,
  disabled = false,
}: CategoryInputProps) {
  const [inputValue, setInputValue] = useState('');
  const inputId = useId();

  const addCategory = () => {
    const trimmedValue = inputValue.trim();
    if (!trimmedValue) return;

    // Split by > to create individual category parts
    const categoryParts = trimmedValue
      .split('>')
      .map((part) => part.trim())
      .filter((part) => part);

    if (categoryParts.length > 0) {
      onChange([...value, ...categoryParts]);
      setInputValue('');
    }
  };

  const removeCategory = (indexToRemove: number) => {
    onChange(value.filter((_, index) => index !== indexToRemove));
  };

  return (
    <div>
      {label && (
        <label htmlFor={inputId} className="label">
          {label}
        </label>
      )}

      {value.length > 0 && (
        <div className="mb-2 flex flex-wrap items-center gap-x-1 gap-y-1">
          {value.map((category, index) => (
            <React.Fragment key={index}>
              <span className="inline-flex items-center gap-1 border border-rule bg-paper px-2 py-0.5 font-mono text-[11px] text-ink">
                {category}
                <button
                  type="button"
                  onClick={() => removeCategory(index)}
                  disabled={disabled}
                  className="text-muted hover:text-redline disabled:opacity-40"
                  aria-label={`Remove category ${category}`}
                >
                  ×
                </button>
              </span>
              {index < value.length - 1 && (
                <span className="font-mono text-[11px] text-muted" aria-hidden="true">
                  ›
                </span>
              )}
            </React.Fragment>
          ))}
        </div>
      )}

      <div className="flex gap-2">
        <input
          id={inputId}
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !disabled) {
              e.preventDefault();
              addCategory();
            }
          }}
          placeholder={placeholder}
          disabled={disabled}
          className={`field ${error ? 'field-invalid' : ''}`}
        />
        <button
          type="button"
          onClick={addCategory}
          disabled={!inputValue.trim() || disabled}
          className="shrink-0 rounded-[2px] border border-ink/70 px-3 font-sans text-xs font-semibold text-ink transition-colors hover:bg-ink hover:text-sheet disabled:pointer-events-none disabled:opacity-40"
        >
          Add
        </button>
      </div>

      {error && <p className="mt-1 font-mono text-[11px] text-redline">{error}</p>}
    </div>
  );
}
