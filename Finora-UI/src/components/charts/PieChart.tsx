import React from 'react';
import {
  PieChart as RechartsPieChart,
  Pie,
  Cell,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import { useTheme } from '../../utils/theme-context';

interface DataItem {
  name: string;
  value: number;
}

interface PieChartProps {
  data: DataItem[];
  title?: string;
  height?: number;
  className?: string;
}

const COLORS = [
  '#3867D6', // primary
  '#48BB78', // success-500
  '#F59E0B', // warning-500
  '#E53E3E', // error-500
  '#8B5CF6', // purple-500
  '#EC4899', // pink-500
  '#14B8A6', // teal-500
  '#0EA5E9', // sky-500
  '#6366F1', // indigo-500
  '#EF4444', // red-500
];

export const PieChart: React.FC<PieChartProps> = ({
  data,
  title,
  height = 300,
  className,
}) => {

  const { theme } = useTheme();
  const isDark = theme === 'dark';

  const tooltipBg    = isDark ? '#1f2937' : '#ffffff';
  const tooltipBorder = isDark ? '#374151' : '#e5e7eb';
  const tooltipText  = isDark ? '#f9fafb' : '#111827';
  const tooltipSub   = isDark ? '#d1d5db' : '#374151';
  const tooltipMuted = isDark ? '#9ca3af' : '#6b7280';

  const filteredData = data.filter(item => item.value > 0);

  if (filteredData.length === 0) {
    return (
      <div className={`flex flex-col items-center justify-center h-${height} ${className}`}>
        <p className="text-neutral-500 dark:text-neutral-400">No data available</p>
      </div>
    );
  }

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      const item = payload[0].payload;
      const total = filteredData.reduce((sum, d) => sum + d.value, 0);
      return (
        <div style={{
          background: tooltipBg,
          border: `1px solid ${tooltipBorder}`,
          borderRadius: 6,
          padding: '10px 14px',
          boxShadow: '0 2px 8px rgba(0,0,0,0.15)',
          maxWidth: 220,
        }}>
          <p style={{ margin: 0, fontWeight: 600, color: tooltipText, fontSize: 14 }}>
            {item.name}
          </p>
          <p style={{ margin: '4px 0 0', color: tooltipSub, fontSize: 13 }}>
            ₹{item.value.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </p>
          <p style={{ margin: '2px 0 0', color: tooltipMuted, fontSize: 12 }}>
            {((item.value / total) * 100).toFixed(1)}%
          </p>
        </div>
      );
    }
    return null;
  };

  const renderLegend = (props: any) => {
    const { payload } = props;
    return (
      <ul className="flex flex-wrap justify-center gap-4 mt-4">
        {payload.map((entry: any, index: number) => (
          <li key={`legend-${index}`} className="flex items-center">
            <span
              className="w-3 h-3 rounded-full mr-2 flex-shrink-0"
              style={{ backgroundColor: entry.color }}
            />
            <span className="text-sm text-neutral-700 dark:text-neutral-300 break-words max-w-[120px]">
              {entry.value}
            </span>
          </li>
        ))}
      </ul>
    );
  };

  const renderCustomizedLabel = ({ cx, cy, midAngle, innerRadius, outerRadius, percent, name }: any) => {
    const RADIAN = Math.PI / 180;
    const radius = innerRadius + (outerRadius - innerRadius) * 0.5;
    const x = cx + radius * Math.cos(-midAngle * RADIAN);
    const y = cy + radius * Math.sin(-midAngle * RADIAN);

    return (
      <text
        x={x}
        y={y}
        fill="white"
        textAnchor={x > cx ? 'start' : 'end'}
        dominantBaseline="central"
        className="text-xs font-medium"
      >
        {`${(percent * 100).toFixed(0)}%`}
      </text>
    );
  };

  return (
    <div className={className}>
      {title && <h3 className="text-base font-medium text-neutral-900 dark:text-neutral-100 mb-4">{title}</h3>}
      <ResponsiveContainer width="100%" height={height}>
        <RechartsPieChart margin={{ top: 20, right: 20, bottom: 20, left: 20 }}>
          <Pie
            data={filteredData}
            dataKey="value"
            nameKey="name"
            cx="50%"
            cy="50%"
            outerRadius={80}
            fill="#8884d8"
            labelLine={false}
            label={renderCustomizedLabel}
          >
            {filteredData.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
            ))}
          </Pie>
          <Tooltip content={<CustomTooltip />} wrapperStyle={{ outline: 'none' }} />
          <Legend content={renderLegend} />
        </RechartsPieChart>
      </ResponsiveContainer>
    </div>
  );
};