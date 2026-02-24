import React from 'react';
import { twMerge } from 'tailwind-merge';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  fullWidth?: boolean;
  icon?: React.ReactNode;
  iconPosition?: 'left' | 'right';
}

export const Input: React.FC<InputProps> = ({
  label,
  error,
  fullWidth = false,
  icon,
  iconPosition = 'left',
  className,
  ...props
}) => {
  const id = props.id || `input-${Math.random().toString(36).substr(2, 9)}`;
  
  return (
    <div className={twMerge(fullWidth && 'w-full')}>
      {label && (
        <label 
          htmlFor={id} 
          className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1"
        >
          {label}
        </label>
      )}
      
      <div className="relative">
        {icon && iconPosition === 'left' && (
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-neutral-500">
            {icon}
          </div>
        )}
        
        <input
          id={id}
          className={twMerge(
            'block px-3 py-2 bg-white border border-neutral-300 rounded-md shadow-sm text-sm',
            'focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500',
            'dark:bg-neutral-900 dark:border-neutral-700 dark:text-neutral-100',
            'disabled:opacity-50 disabled:cursor-not-allowed',
            error && 'border-error-500 focus:ring-error-500 focus:border-error-500',
            icon && iconPosition === 'left' && 'pl-10',
            icon && iconPosition === 'right' && 'pr-10',
            fullWidth && 'w-full',
            className
          )}
          {...props}
        />
        
        {icon && iconPosition === 'right' && (
          <div className="absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none text-neutral-500">
            {icon}
          </div>
        )}
      </div>
      
      {error && (
        <p className="mt-1 text-sm text-error-500">{error}</p>
      )}
    </div>
  );
};