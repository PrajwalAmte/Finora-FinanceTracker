import React from 'react';
import { HelpCircle, X } from 'lucide-react';

interface TooltipInfo {
  title: string;
  description: string;
  steps?: string[];
}

interface FeatureTooltipProps {
  info: TooltipInfo;
  isOpen: boolean;
  onClose: () => void;
  position?: 'top' | 'bottom' | 'left' | 'right';
}

export const FeatureTooltip: React.FC<FeatureTooltipProps> = ({
  info,
  isOpen,
  onClose,
  position = 'bottom',
}) => {
  if (!isOpen) return null;

  const positionClasses = {
    top: 'bottom-full mb-2',
    bottom: 'top-full mt-2',
    left: 'right-full mr-2',
    right: 'left-full ml-2',
  };

  return (
    <div
      className={`absolute ${positionClasses[position]} z-30 w-72 p-4 bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 rounded-lg shadow-xl`}
    >
      <div className="flex items-start justify-between mb-2">
        <h4 className="text-sm font-semibold text-neutral-900 dark:text-white">{info.title}</h4>
        <button
          onClick={onClose}
          className="p-0.5 rounded hover:bg-neutral-100 dark:hover:bg-neutral-700"
        >
          <X size={14} className="text-neutral-400" />
        </button>
      </div>
      <p className="text-xs text-neutral-600 dark:text-neutral-400 mb-3">{info.description}</p>
      {info.steps && (
        <ol className="space-y-1.5">
          {info.steps.map((step, idx) => (
            <li key={idx} className="flex items-start gap-2 text-xs text-neutral-600 dark:text-neutral-400">
              <span className="shrink-0 w-4 h-4 rounded-full bg-primary-100 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400 flex items-center justify-center text-[10px] font-medium">
                {idx + 1}
              </span>
              {step}
            </li>
          ))}
        </ol>
      )}
    </div>
  );
};

interface InfoButtonProps {
  onClick: () => void;
  className?: string;
}

export const InfoButton: React.FC<InfoButtonProps> = ({ onClick, className }) => {
  return (
    <button
      onClick={onClick}
      className={`p-1 rounded-full hover:bg-neutral-100 dark:hover:bg-neutral-700 transition-colors ${className || ''}`}
      title="Learn more"
    >
      <HelpCircle size={16} className="text-neutral-400 hover:text-neutral-600 dark:hover:text-neutral-300" />
    </button>
  );
};
