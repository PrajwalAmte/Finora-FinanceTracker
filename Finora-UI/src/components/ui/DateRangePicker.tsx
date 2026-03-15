import React, { useState, useRef, useEffect } from 'react';
import { Calendar, ChevronDown } from 'lucide-react';

export type DatePreset = 'thisMonth' | 'lastMonth' | 'last3Months' | 'last6Months' | 'thisYear' | 'lastYear' | 'custom';

export interface DateRange {
  startDate: string;
  endDate: string;
  preset: DatePreset;
}

interface DateRangePickerProps {
  value: DateRange;
  onChange: (range: DateRange) => void;
}

type PresetKey = DatePreset;

const presets: { key: PresetKey; label: string }[] = [
  { key: 'thisMonth', label: 'This Month' },
  { key: 'lastMonth', label: 'Last Month' },
  { key: 'last3Months', label: 'Last 3 Months' },
  { key: 'last6Months', label: 'Last 6 Months' },
  { key: 'thisYear', label: 'This Year' },
  { key: 'lastYear', label: 'Last Year' },
  { key: 'custom', label: 'Custom Range' },
];

function getPresetDates(key: PresetKey): { start: string; end: string } | null {
  const today = new Date();
  const year = today.getFullYear();
  const month = today.getMonth();

  switch (key) {
    case 'thisMonth':
      return {
        start: new Date(year, month, 1).toISOString().split('T')[0],
        end: today.toISOString().split('T')[0],
      };
    case 'lastMonth':
      return {
        start: new Date(year, month - 1, 1).toISOString().split('T')[0],
        end: new Date(year, month, 0).toISOString().split('T')[0],
      };
    case 'last3Months':
      return {
        start: new Date(year, month - 2, 1).toISOString().split('T')[0],
        end: today.toISOString().split('T')[0],
      };
    case 'last6Months':
      return {
        start: new Date(year, month - 5, 1).toISOString().split('T')[0],
        end: today.toISOString().split('T')[0],
      };
    case 'thisYear':
      return {
        start: new Date(year, 0, 1).toISOString().split('T')[0],
        end: today.toISOString().split('T')[0],
      };
    case 'lastYear':
      return {
        start: new Date(year - 1, 0, 1).toISOString().split('T')[0],
        end: new Date(year - 1, 11, 31).toISOString().split('T')[0],
      };
    default:
      return null;
  }
}

function formatDisplayDate(dateStr: string): string {
  if (!dateStr) return '';
  const [year, month, day] = dateStr.split('-').map(Number);
  const date = new Date(year, month - 1, day);
  return date.toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}

export const DateRangePicker: React.FC<DateRangePickerProps> = ({ value, onChange }) => {
  const { startDate, endDate, preset: currentPreset } = value;
  const [isOpen, setIsOpen] = useState(false);
  const [selectedPreset, setSelectedPreset] = useState<PresetKey>(currentPreset);
  const [customStart, setCustomStart] = useState(startDate);
  const [customEnd, setCustomEnd] = useState(endDate);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const handlePresetClick = (key: PresetKey) => {
    setSelectedPreset(key);
    if (key !== 'custom') {
      const dates = getPresetDates(key);
      if (dates) {
        onChange({ startDate: dates.start, endDate: dates.end, preset: key });
        setIsOpen(false);
      }
    }
  };

  const handleApplyCustom = () => {
    onChange({ startDate: customStart, endDate: customEnd, preset: 'custom' });
    setIsOpen(false);
  };

  const displayText = `${formatDisplayDate(startDate)} - ${formatDisplayDate(endDate)}`;

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 px-3 py-2 text-sm border border-neutral-200 dark:border-neutral-700 rounded-lg bg-white dark:bg-neutral-800 hover:bg-neutral-50 dark:hover:bg-neutral-700 transition-colors"
      >
        <Calendar size={16} className="text-neutral-500" />
        <span className="text-neutral-700 dark:text-neutral-300">{displayText}</span>
        <ChevronDown size={16} className="text-neutral-400" />
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-72 bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 rounded-lg shadow-lg z-20 overflow-hidden">
          <div className="p-2 space-y-1">
            {presets.map((preset) => (
              <button
                key={preset.key}
                onClick={() => handlePresetClick(preset.key)}
                className={`w-full text-left px-3 py-2 text-sm rounded-md transition-colors ${
                  selectedPreset === preset.key
                    ? 'bg-primary-50 dark:bg-primary-900/30 text-primary-700 dark:text-primary-300'
                    : 'text-neutral-700 dark:text-neutral-300 hover:bg-neutral-100 dark:hover:bg-neutral-700'
                }`}
              >
                {preset.label}
              </button>
            ))}
          </div>

          {selectedPreset === 'custom' && (
            <div className="border-t border-neutral-200 dark:border-neutral-700 p-3 space-y-3">
              <div>
                <label className="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">
                  From
                </label>
                <input
                  type="date"
                  value={customStart}
                  onChange={(e) => setCustomStart(e.target.value)}
                  className="w-full px-3 py-2 text-sm border border-neutral-200 dark:border-neutral-600 rounded-md bg-white dark:bg-neutral-700 text-neutral-900 dark:text-white"
                />
              </div>
              <div>
                <label className="block text-xs font-medium text-neutral-500 dark:text-neutral-400 mb-1">
                  To
                </label>
                <input
                  type="date"
                  value={customEnd}
                  onChange={(e) => setCustomEnd(e.target.value)}
                  className="w-full px-3 py-2 text-sm border border-neutral-200 dark:border-neutral-600 rounded-md bg-white dark:bg-neutral-700 text-neutral-900 dark:text-white"
                />
              </div>
              <button
                onClick={handleApplyCustom}
                className="w-full py-2 text-sm font-medium bg-primary-600 hover:bg-primary-700 text-white rounded-md transition-colors"
              >
                Apply
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
