import React from 'react';
import { Card } from '../ui/Card';
import { formatCurrency, getStatusColorClass } from '../../utils/formatters';

interface DashboardMetricCardProps {
  title: string;
  value: number;
  changeValue?: number;
  changeLabel?: string;
  icon: React.ReactNode;
  isLoading?: boolean;
  currency?: boolean;
  onClick?: () => void;
}

export const DashboardMetricCard: React.FC<DashboardMetricCardProps> = ({
  title,
  value,
  changeValue,
  changeLabel,
  icon,
  isLoading = false,
  currency = true,
  onClick,
}) => {
  return (
    <Card
      className="h-full transition-transform duration-200 hover:scale-102 cursor-pointer"
      isLoading={isLoading}
      icon={icon}
      title={title}
      onHeaderClick={onClick}
    >
      <div className="space-y-2">
        <div className="text-2xl font-bold text-neutral-900 dark:text-white">
          {currency ? formatCurrency(value) : value}
        </div>
        
        {(changeValue !== undefined && changeLabel) && (
          <div className="flex items-center">
            <span className={`text-sm ${getStatusColorClass(changeValue)}`}>
              {changeValue > 0 ? '↑' : changeValue < 0 ? '↓' : ''}
              {' '}
              {Math.abs(changeValue).toFixed(2)}%
            </span>
            <span className="text-sm text-neutral-500 dark:text-neutral-400 ml-1">
              {changeLabel}
            </span>
          </div>
        )}
      </div>
    </Card>
  );
};