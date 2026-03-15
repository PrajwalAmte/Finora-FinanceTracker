import { format, parseISO } from 'date-fns';

export const formatCurrency = (amount: number | string, locale = 'en-IN', currency = 'INR'): string => {
  const numAmount = typeof amount === 'string' ? parseFloat(amount) : amount;
  
  if (isNaN(numAmount)) {
    return '---';
  }
  
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(numAmount);
};

export const formatPercentage = (value: number | string, decimalPlaces = 2): string => {
  const numValue = typeof value === 'string' ? parseFloat(value) : value;
  
  if (isNaN(numValue)) {
    return '---';
  }
  
  return `${(numValue).toFixed(decimalPlaces)}%`;
};

export const formatDate = (
  dateString: string | undefined | null, 
  dateFormat = 'MMM dd, yyyy'
): string => {
  if (!dateString) return '---';
  
  try {
    return format(parseISO(dateString), dateFormat);
  } catch {
    return '---';
  }
};

export const formatNumber = (number: number | string, maximumFractionDigits = 1): string => {
  const num = typeof number === 'string' ? parseFloat(number) : number;
  
  if (isNaN(num)) {
    return '---';
  }
  
  const absNum = Math.abs(num);
  
  if (absNum >= 1000000000) {
    return `${(num / 1000000000).toFixed(maximumFractionDigits)}B`;
  }
  
  if (absNum >= 1000000) {
    return `${(num / 1000000).toFixed(maximumFractionDigits)}M`;
  }
  
  if (absNum >= 1000) {
    return `${(num / 1000).toFixed(maximumFractionDigits)}K`;
  }
  
  return num.toFixed(maximumFractionDigits);
};

export const getStatusColorClass = (value: number): string => {
  if (value > 0) {
    return 'text-success-600';
  } else if (value < 0) {
    return 'text-error-600';
  }
  return 'text-neutral-500';
};

export const getStatusBgClass = (value: number): string => {
  if (value > 0) {
    return 'bg-success-50 text-success-600';
  } else if (value < 0) {
    return 'bg-error-50 text-error-600';
  }
  return 'bg-neutral-50 text-neutral-500';
};