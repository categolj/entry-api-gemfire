import React, { useState, KeyboardEvent } from 'react';

interface TagInputProps {
  label?: string;
  value: string[];
  onChange: (tags: string[]) => void;
  placeholder?: string;
  error?: string;
  disabled?: boolean;
}

export function TagInput({
  label,
  value,
  onChange,
  placeholder = 'Add a tag, press Enter',
  error,
  disabled = false,
}: TagInputProps) {
  const [inputValue, setInputValue] = useState('');
  const inputId = `tag-input-${Math.random().toString(36).substr(2, 9)}`;

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (disabled) return;
    if (e.key === 'Enter') {
      e.preventDefault();
      const trimmedValue = inputValue.trim();
      if (trimmedValue) {
        onChange([...value, trimmedValue]);
        setInputValue('');
      }
    } else if (e.key === 'Backspace' && !inputValue && value.length > 0) {
      onChange(value.slice(0, -1));
    }
  };

  const removeTag = (indexToRemove: number) => {
    onChange(value.filter((_, index) => index !== indexToRemove));
  };

  return (
    <div>
      {label && (
        <label htmlFor={inputId} className="label">
          {label}
        </label>
      )}
      <div
        className={`flex flex-wrap items-center gap-1.5 rounded-[2px] border bg-sheet p-2 transition-colors focus-within:border-pencil focus-within:shadow-[0_0_0_3px_#DFE2F8] ${
          error ? 'border-redline' : 'border-rule'
        } ${disabled ? 'bg-paper' : ''}`}
      >
        {value.map((tag, index) => (
          <span
            key={index}
            className="inline-flex items-center gap-1.5 border border-rule bg-paper px-2 py-0.5 font-mono text-[11px] text-ink"
          >
            <span className="text-pencil">#</span>
            {tag}
            <button
              type="button"
              onClick={() => removeTag(index)}
              disabled={disabled}
              className="text-muted hover:text-redline disabled:opacity-40"
              aria-label={`Remove tag ${tag}`}
            >
              ×
            </button>
          </span>
        ))}
        <input
          id={inputId}
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={value.length === 0 ? placeholder : ''}
          disabled={disabled}
          className="min-w-[9rem] flex-1 border-none bg-transparent px-1 py-0.5 font-sans text-sm outline-none disabled:cursor-not-allowed"
        />
      </div>
      {error && <p className="mt-1 font-mono text-[11px] text-redline">{error}</p>}
    </div>
  );
}
