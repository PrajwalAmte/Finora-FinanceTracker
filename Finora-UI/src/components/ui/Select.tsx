import React from 'react';
import { twMerge } from 'tailwind-merge';

interface SelectOption {
  value: string;
  label: string;
}

interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  options: SelectOption[];
  label?: string;
  error?: string;
  fullWidth?: boolean;
  className?: string;
}

export const Select: React.FC<SelectProps> = ({
  options,
  label,
  error,
  fullWidth = false,
  className,
  ...props
}) => {
  const id = props.id || `select-${Math.random().toString(36).substr(2, 9)}`;
  
  return (
    <div className={twMerge(fullWidth && 'w-full', className)}>
      {label && (
        <label 
          htmlFor={id} 
          className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1"
        >
          {label}
        </label>
      )}
      
      <select
        id={id}
        className={twMerge(
          'block px-3 py-2 bg-white border border-neutral-300 rounded-md shadow-sm text-sm',
          'focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500',
          'dark:bg-neutral-900 dark:border-neutral-700 dark:text-neutral-100',
          'disabled:opacity-50 disabled:cursor-not-allowed',
          error && 'border-error-500 focus:ring-error-500 focus:border-error-500',
          fullWidth && 'w-full',
        )}
        {...props}
      >
        {options.map(option => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      
      {error && (
        <p className="mt-1 text-sm text-error-500">{error}</p>
      )}
    </div>
  );
};