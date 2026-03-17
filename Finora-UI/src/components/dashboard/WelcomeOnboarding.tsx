import React from 'react';
import { useNavigate } from 'react-router-dom';
import {
  TrendingUp,
  Receipt,
  CreditCard,
  PiggyBank,
  Upload,
  Shield,
  ArrowRight,
  X,
} from 'lucide-react';
import { Button } from '../ui/Button';

interface OnboardingStep {
  icon: React.ReactNode;
  title: string;
  description: string;
  action: string;
  path: string;
  color: string;
}

const steps: OnboardingStep[] = [
  {
    icon: <Receipt size={24} />,
    title: 'Track Expenses',
    description: 'Log your daily spending to understand where your money goes',
    action: 'Add First Expense',
    path: '/expenses',
    color: 'text-orange-500 bg-orange-50 dark:bg-orange-900/20',
  },
  {
    icon: <TrendingUp size={24} />,
    title: 'Manage Investments',
    description: 'Track stocks, mutual funds, and other investments in one place',
    action: 'Add Investment',
    path: '/investments',
    color: 'text-blue-500 bg-blue-50 dark:bg-blue-900/20',
  },
  {
    icon: <PiggyBank size={24} />,
    title: 'Monitor SIPs',
    description: 'Keep track of your systematic investment plans',
    action: 'Add SIP',
    path: '/sips',
    color: 'text-green-500 bg-green-50 dark:bg-green-900/20',
  },
  {
    icon: <CreditCard size={24} />,
    title: 'Track Loans',
    description: 'Monitor your loans and EMI payments',
    action: 'Add Loan',
    path: '/loans',
    color: 'text-red-500 bg-red-50 dark:bg-red-900/20',
  },
];

interface WelcomeOnboardingProps {
  userName?: string;
  hasData: boolean;
  onDismiss: () => void;
}

export const WelcomeOnboarding: React.FC<WelcomeOnboardingProps> = ({
  userName,
  hasData,
  onDismiss,
}) => {
  const navigate = useNavigate();

  if (hasData) return null;

  return (
    <div className="mb-8 bg-gradient-to-r from-primary-600 to-primary-700 dark:from-primary-700 dark:to-primary-800 rounded-xl p-6 text-white relative overflow-hidden">
      <button
        onClick={onDismiss}
        className="absolute top-4 right-4 p-1 rounded-full hover:bg-white/10 transition-colors"
      >
        <X size={18} />
      </button>

      <div className="relative z-10">
        <h2 className="text-2xl font-bold mb-2">
          Welcome{userName ? `, ${userName}` : ''}!
        </h2>
        <p className="text-primary-100 mb-6 max-w-xl">
          Get started by adding your financial data. Pick any section below to begin tracking your finances.
        </p>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {steps.map((step) => (
            <button
              key={step.title}
              onClick={() => navigate(step.path)}
              className="flex flex-col items-start p-4 bg-white/10 hover:bg-white/20 backdrop-blur-sm rounded-lg text-left transition-all group"
            >
              <div className={`p-2 rounded-lg ${step.color} mb-3`}>
                {step.icon}
              </div>
              <h3 className="font-semibold text-sm mb-1">{step.title}</h3>
              <p className="text-xs text-primary-100 mb-3 line-clamp-2">{step.description}</p>
              <span className="text-xs font-medium flex items-center gap-1 mt-auto text-primary-200 group-hover:text-white">
                {step.action}
                <ArrowRight size={12} className="transition-transform group-hover:translate-x-1" />
              </span>
            </button>
          ))}
        </div>
      </div>

      <div className="absolute -right-20 -bottom-20 w-64 h-64 bg-white/5 rounded-full" />
      <div className="absolute -right-10 -bottom-10 w-48 h-48 bg-white/5 rounded-full" />
    </div>
  );
};

interface QuickActionsProps {
  onImportClick: () => void;
}

export const QuickActions: React.FC<QuickActionsProps> = ({ onImportClick }) => {
  const navigate = useNavigate();

  return (
    <div className="flex flex-wrap gap-3 mb-6">
      <Button
        variant="outline"
        size="sm"
        iconLeft={<Upload size={16} />}
        onClick={onImportClick}
      >
        Import Statement
      </Button>
      <Button
        variant="ghost"
        size="sm"
        iconLeft={<Shield size={16} />}
        onClick={() => navigate('/profile')}
      >
        Enable Vault
      </Button>
    </div>
  );
};
