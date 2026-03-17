import React from 'react';
import {
  BarChart as RechartsBarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { useTheme } from '../../utils/theme-context';

interface DataItem {
  name: string;
  value: number;
  [key: string]: unknown;
}

interface BarChartProps {
  data: DataItem[];
  xAxisKey?: string;
  bars: Array<{
    dataKey: string;
    name: string;
    color: string;
  }>;
  title?: string;
  height?: number;
  className?: string;
  stacked?: boolean;
}

export const BarChart: React.FC<BarChartProps> = ({
  data,
  xAxisKey = 'name',
  bars,
  title,
  height = 300,
  className,
  stacked = false,
}) => {
  const { theme } = useTheme();
  const isDark = theme === 'dark';

  const tooltipBg     = isDark ? '#1f2937' : '#ffffff';
  const tooltipBorder = isDark ? '#374151' : '#e5e7eb';
  const tooltipText   = isDark ? '#f9fafb' : '#111827';
  const tickColor     = isDark ? '#9ca3af' : '#6b7280';
  const gridColor     = isDark ? '#374151' : '#e5e7eb';

  // If no data
  if (!data || data.length === 0) {
    return (
      <div className={`flex flex-col items-center justify-center h-${height} ${className}`}>
        <p className="text-neutral-500 dark:text-neutral-400">No data available</p>
      </div>
    );
  }

  // Custom tooltip
  interface TooltipEntry {
    name: string;
    value: number;
    color: string;
  }

  interface CustomTooltipProps {
    active?: boolean;
    payload?: TooltipEntry[];
    label?: string;
  }

  const CustomTooltip = ({ active, payload, label }: CustomTooltipProps) => {
    if (active && payload && payload.length) {
      return (
        <div style={{
          background: tooltipBg,
          border: `1px solid ${tooltipBorder}`,
          borderRadius: 6,
          padding: '10px 14px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
          maxWidth: 240,
        }}>
          <p style={{ margin: 0, fontWeight: 600, color: tooltipText, fontSize: 14 }}>{label}</p>
          {payload.map((entry: TooltipEntry, index: number) => (
            <p key={`tooltip-${index}`} style={{ margin: '4px 0 0', color: entry.color, fontSize: 13 }}>
              {entry.name}: ₹{entry.value.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            </p>
          ))}
        </div>
      );
    }
    return null;
  };

  return (
    <div className={className}>
      {title && <h3 className="text-base font-medium text-neutral-900 dark:text-neutral-100 mb-4">{title}</h3>}
      <ResponsiveContainer width="100%" height={height}>
        <RechartsBarChart 
          data={data} 
          margin={{ top: 20, right: 30, left: 40, bottom: 60 }}
        >
          <CartesianGrid strokeDasharray="3 3" stroke={gridColor} />
          <XAxis 
            dataKey={xAxisKey} 
            tick={{ fill: tickColor }} 
            tickLine={{ stroke: tickColor }} 
            axisLine={{ stroke: tickColor }}
            height={60}
            interval={0}
            angle={-45}
            textAnchor="end"
          />
          <YAxis 
            tick={{ fill: tickColor }} 
            tickLine={{ stroke: tickColor }} 
            axisLine={{ stroke: tickColor }}
            tickFormatter={(value) => `₹${value.toLocaleString('en-IN', { maximumFractionDigits: 0 })}`}
            width={80}
          />
          <Tooltip content={<CustomTooltip />} wrapperStyle={{ outline: 'none' }} />
          <Legend wrapperStyle={{ paddingTop: '20px' }} />
          {bars.map((bar, index) => (
            <Bar
              key={`bar-${index}`}
              dataKey={bar.dataKey}
              name={bar.name}
              stackId={stacked ? 'stack' : undefined}
              fill={bar.color}
              radius={[4, 4, 0, 0]}
            />
          ))}
        </RechartsBarChart>
      </ResponsiveContainer>
    </div>
  );
};