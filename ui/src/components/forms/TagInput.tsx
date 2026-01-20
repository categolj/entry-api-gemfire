import React, { useState, KeyboardEvent } from 'react';

interface TagInputProps {
  label?: string;
  value: string[];
  onChange: (tags: string[]) => void;
  placeholder?: string;
  error?: string;
  disabled?: boolean;
}

export function TagInput({ label, value, onChange, placeholder = "Add tags and press Enter", error, disabled = false }: TagInputProps) {
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
        <label htmlFor={inputId} className="block text-sm font-medium text-black mb-1">
          {label}
        </label>
      )}
      <div className={`flex flex-wrap gap-2 p-2 border min-h-[2.5rem] ${error ? 'border-red-400' : 'border-gray-300'} ${disabled ? 'bg-gray-50' : ''} focus-within:border-black transition-colors`}>
        {value.map((tag, index) => (
          <span
            key={index}
            className="inline-flex items-center px-2 py-1 text-xs font-medium bg-gray-100 text-black border border-gray-300"
          >
            {tag}
            <button
              type="button"
              onClick={() => removeTag(index)}
              disabled={disabled}
              className="ml-1 text-gray-500 hover:text-black focus:outline-none disabled:opacity-40 disabled:cursor-not-allowed"
            >
              x
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
          className="flex-1 min-w-[120px] border-none outline-none bg-transparent disabled:cursor-not-allowed"
        />
      </div>
      {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
    </div>
  );
}