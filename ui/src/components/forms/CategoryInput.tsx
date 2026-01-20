import React, { useState } from 'react';

interface CategoryInputProps {
  label?: string;
  value: string[];
  onChange: (categories: string[]) => void;
  placeholder?: string;
  error?: string;
  disabled?: boolean;
}

export function CategoryInput({ label, value, onChange, placeholder = "Add categories (e.g., Tech>Programming or individual categories)", error, disabled = false }: CategoryInputProps) {
  const [inputValue, setInputValue] = useState('');
  const inputId = `category-input-${Math.random().toString(36).substr(2, 9)}`;

  const addCategory = () => {
    console.log('addCategory called with input:', inputValue);
    const trimmedValue = inputValue.trim();
    if (trimmedValue) {
      // Split by > to create individual category parts
      const categoryParts = trimmedValue.split('>').map(part => part.trim()).filter(part => part);
      if (categoryParts.length > 0) {
        // Add all category parts (allow duplicates)
        const newCategories = [...value, ...categoryParts];
        console.log('Adding categories:', categoryParts, 'Current categories:', value);
        onChange(newCategories);
        setInputValue('');
      }
    } else {
      console.log('No input value to add');
    }
  };

  const handleButtonClick = (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    console.log('Button clicked');
    addCategory();
  };

  const removeCategory = (indexToRemove: number) => {
    onChange(value.filter((_, index) => index !== indexToRemove));
  };

  return (
    <div>
      {label && (
        <label htmlFor={inputId} className="block text-sm font-medium text-black mb-1">
          {label}
        </label>
      )}

      {/* Existing categories */}
      {value.length > 0 && (
        <div className="mb-3">
          <div className="flex flex-wrap items-center gap-1">
            {value.map((category, index) => (
              <React.Fragment key={index}>
                <span className="inline-flex items-center px-3 py-1 text-sm font-medium bg-gray-100 text-black border border-gray-300">
                  {category}
                  <button
                    type="button"
                    onClick={() => removeCategory(index)}
                    disabled={disabled}
                    className="ml-2 text-gray-500 hover:text-black focus:outline-none disabled:opacity-40 disabled:cursor-not-allowed"
                    title="Remove category"
                  >
                    x
                  </button>
                </span>
                {index < value.length - 1 && (
                  <span className="text-gray-500 font-medium text-sm">
                    {'>'}
                  </span>
                )}
              </React.Fragment>
            ))}
          </div>
        </div>
      )}

      {/* Input form */}
      <div className="flex gap-2">
        <input
          id={inputId}
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !disabled) {
              e.preventDefault();
              console.log('Enter key pressed');
              addCategory();
            }
          }}
          placeholder={placeholder}
          disabled={disabled}
          className={`flex-1 px-3 py-2 border placeholder-gray-400 focus:outline-none focus:border-black transition-colors disabled:bg-gray-50 disabled:cursor-not-allowed ${error ? 'border-red-400' : 'border-gray-300'}`}
        />
        <button
          type="button"
          onClick={handleButtonClick}
          disabled={!inputValue.trim() || disabled}
          className="px-4 py-2 bg-black text-white hover:opacity-80 focus:outline-none disabled:opacity-40 disabled:cursor-not-allowed transition-opacity"
        >
          Add
        </button>
      </div>

      {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
    </div>
  );
}