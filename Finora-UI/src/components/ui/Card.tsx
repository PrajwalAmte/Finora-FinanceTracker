import React from 'react';
import { twMerge } from 'tailwind-merge';

interface CardProps {
  title?: string;
  subtitle?: string;
  icon?: React.ReactNode;
  className?: string;
  headerClassName?: string;
  bodyClassName?: string;
  footerClassName?: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
  isLoading?: boolean;
  onHeaderClick?: () => void;
}

export const Card: React.FC<CardProps> = ({
  title,
  subtitle,
  icon,
  className,
  headerClassName,
  bodyClassName,
  footerClassName,
  children,
  footer,
  isLoading = false,
  onHeaderClick,
}) => {
  return (
    <div 
      className={twMerge(
        'bg-white border border-neutral-200 rounded-lg overflow-hidden shadow-card transition-shadow hover:shadow-card-hover',
        'dark:bg-neutral-800 dark:border-neutral-700 dark:shadow-card-dark dark:hover:shadow-card-hover-dark',
        'animate-fade-in',
        className
      )}
    >
      {(title || subtitle || icon) && (
        <div 
          className={twMerge(
            'flex items-center justify-between px-5 py-4 border-b border-neutral-200 dark:border-neutral-700',
            onHeaderClick && 'cursor-pointer hover:bg-neutral-50 dark:hover:bg-neutral-700',
            headerClassName
          )}
          onClick={onHeaderClick}
        >
          <div className="flex items-center space-x-3">
            {icon && <div className="flex-shrink-0 text-primary-500 dark:text-primary-400">{icon}</div>}
            <div>
              {title && <h3 className="text-lg font-medium text-neutral-900 dark:text-neutral-100">{title}</h3>}
              {subtitle && <p className="text-sm text-neutral-500 dark:text-neutral-400">{subtitle}</p>}
            </div>
          </div>
        </div>
      )}
      
      <div className={twMerge('p-5', bodyClassName)}>
        {isLoading ? (
          <div className="flex justify-center items-center py-10">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
          </div>
        ) : (
          children
        )}
      </div>
      
      {footer && (
        <div className={twMerge('px-5 py-4 bg-neutral-50 border-t border-neutral-200 dark:bg-neutral-800 dark:border-neutral-700', footerClassName)}>
          {footer}
        </div>
      )}
    </div>
  );
};