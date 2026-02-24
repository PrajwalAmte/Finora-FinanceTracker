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

interface DataItem {
  name: string;
  value: number;
  [key: string]: any;
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
  // If no data
  if (!data || data.length === 0) {
    return (
      <div className={`flex flex-col items-center justify-center h-${height} ${className}`}>
        <p className="text-neutral-500 dark:text-neutral-400">No data available</p>
      </div>
    );
  }

  // Custom tooltip
  const CustomTooltip = ({ active, payload, label }: any) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-white dark:bg-neutral-800 p-3 border border-neutral-200 dark:border-neutral-700 rounded shadow">
          <p className="font-medium truncate max-w-[200px]">{label}</p>
          {payload.map((entry: any, index: number) => (
            <p key={`tooltip-${index}`} style={{ color: entry.color }} className="text-sm">
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
          <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
          <XAxis 
            dataKey={xAxisKey} 
            tick={{ fill: '#6B7280' }} 
            tickLine={{ stroke: '#6B7280' }} 
            axisLine={{ stroke: '#9CA3AF' }}
            height={60}
            interval={0}
            angle={-45}
            textAnchor="end"
          />
          <YAxis 
            tick={{ fill: '#6B7280' }} 
            tickLine={{ stroke: '#6B7280' }} 
            axisLine={{ stroke: '#9CA3AF' }}
            tickFormatter={(value) => `₹${value.toLocaleString('en-IN', { maximumFractionDigits: 0 })}`}
            width={80}
          />
          <Tooltip content={<CustomTooltip />} />
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