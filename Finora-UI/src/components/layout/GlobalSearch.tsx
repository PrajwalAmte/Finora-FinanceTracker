import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, X, TrendingUp, CreditCard, Receipt, PiggyBank, ArrowRight } from 'lucide-react';
import { expenseApi } from '../../api/expenseApi';
import { investmentApi } from '../../api/investmentApi';
import { loanApi } from '../../api/loanApi';
import { sipApi } from '../../api/sipApi';
import { formatCurrency } from '../../utils/formatters';

interface SearchResult {
  id: number;
  type: 'expense' | 'investment' | 'loan' | 'sip';
  title: string;
  subtitle: string;
  value?: number;
}

interface GlobalSearchProps {
  isOpen: boolean;
  onClose: () => void;
}

const typeConfig = {
  expense: { icon: Receipt, color: 'text-orange-500', bg: 'bg-orange-50 dark:bg-orange-900/20', path: '/expenses' },
  investment: { icon: TrendingUp, color: 'text-blue-500', bg: 'bg-blue-50 dark:bg-blue-900/20', path: '/investments' },
  loan: { icon: CreditCard, color: 'text-red-500', bg: 'bg-red-50 dark:bg-red-900/20', path: '/loans' },
  sip: { icon: PiggyBank, color: 'text-green-500', bg: 'bg-green-50 dark:bg-green-900/20', path: '/sips' },
};

export const GlobalSearch: React.FC<GlobalSearchProps> = ({ isOpen, onClose }) => {
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);
  const [query, setQuery] = useState('');
  const [results, setResults] = useState<SearchResult[]>([]);
  const [isLoading, setIsLoading] = useState(false);
  const [selectedIndex, setSelectedIndex] = useState(0);

  useEffect(() => {
    if (isOpen) {
      inputRef.current?.focus();
      setQuery('');
      setResults([]);
      setSelectedIndex(0);
    }
  }, [isOpen]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'k' && (e.metaKey || e.ctrlKey)) {
        e.preventDefault();
        if (!isOpen) {
          onClose();
        }
      }
      if (e.key === 'Escape' && isOpen) {
        onClose();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  useEffect(() => {
    if (!query.trim()) {
      setResults([]);
      return;
    }

    const searchData = async () => {
      setIsLoading(true);
      try {
        const [expenses, investments, loans, sips] = await Promise.all([
          expenseApi.getAll(),
          investmentApi.getAll(),
          loanApi.getAll(),
          sipApi.getAll(),
        ]);

        const q = query.toLowerCase();
        const matched: SearchResult[] = [];

        expenses.forEach((e) => {
          if (e.description.toLowerCase().includes(q) || e.category.toLowerCase().includes(q)) {
            matched.push({
              id: e.id!,
              type: 'expense',
              title: e.description,
              subtitle: e.category,
              value: e.amount,
            });
          }
        });

        investments.forEach((i) => {
          if (i.name.toLowerCase().includes(q) || i.symbol.toLowerCase().includes(q)) {
            matched.push({
              id: i.id!,
              type: 'investment',
              title: i.name,
              subtitle: i.symbol,
              value: i.currentValue || i.quantity * i.currentPrice,
            });
          }
        });

        loans.forEach((l) => {
          if (l.name.toLowerCase().includes(q)) {
            matched.push({
              id: l.id!,
              type: 'loan',
              title: l.name,
              subtitle: `${l.interestRate}% ${l.interestType}`,
              value: l.currentBalance,
            });
          }
        });

        sips.forEach((s) => {
          if (s.name.toLowerCase().includes(q) || (s.schemeCode && s.schemeCode.toLowerCase().includes(q))) {
            matched.push({
              id: s.id!,
              type: 'sip',
              title: s.name,
              subtitle: s.schemeCode || 'Manual SIP',
              value: s.currentValue || 0,
            });
          }
        });

        setResults(matched.slice(0, 10));
        setSelectedIndex(0);
      } catch {
        setResults([]);
      } finally {
        setIsLoading(false);
      }
    };

    const debounce = setTimeout(searchData, 200);
    return () => clearTimeout(debounce);
  }, [query]);

  const handleSelect = (result: SearchResult) => {
    navigate(typeConfig[result.type].path);
    onClose();
  };

  const handleKeyNav = (e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setSelectedIndex((prev) => Math.min(prev + 1, results.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setSelectedIndex((prev) => Math.max(prev - 1, 0));
    } else if (e.key === 'Enter' && results[selectedIndex]) {
      handleSelect(results[selectedIndex]);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="fixed inset-0 bg-black/40 backdrop-blur-sm" onClick={onClose} />
      <div className="relative min-h-screen flex items-start justify-center pt-[15vh] px-4">
        <div className="w-full max-w-xl bg-white dark:bg-neutral-800 rounded-xl shadow-2xl overflow-hidden">
          <div className="flex items-center px-4 border-b border-neutral-200 dark:border-neutral-700">
            <Search size={20} className="text-neutral-400 shrink-0" />
            <input
              ref={inputRef}
              type="text"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyDown={handleKeyNav}
              placeholder="Search expenses, investments, loans, SIPs..."
              className="flex-1 px-3 py-4 bg-transparent border-0 outline-none text-neutral-900 dark:text-white placeholder-neutral-400"
            />
            <button
              onClick={onClose}
              className="p-1.5 rounded hover:bg-neutral-100 dark:hover:bg-neutral-700"
            >
              <X size={18} className="text-neutral-500" />
            </button>
          </div>

          <div className="max-h-80 overflow-y-auto">
            {isLoading ? (
              <div className="py-8 text-center text-neutral-500">Searching...</div>
            ) : results.length > 0 ? (
              <ul className="py-2">
                {results.map((result, idx) => {
                  const config = typeConfig[result.type];
                  const Icon = config.icon;
                  return (
                    <li key={`${result.type}-${result.id}`}>
                      <button
                        onClick={() => handleSelect(result)}
                        className={`w-full flex items-center gap-3 px-4 py-3 text-left transition-colors ${
                          idx === selectedIndex
                            ? 'bg-primary-50 dark:bg-primary-900/30'
                            : 'hover:bg-neutral-50 dark:hover:bg-neutral-700/50'
                        }`}
                      >
                        <div className={`w-9 h-9 rounded-lg ${config.bg} flex items-center justify-center shrink-0`}>
                          <Icon size={18} className={config.color} />
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="text-sm font-medium text-neutral-900 dark:text-white truncate">
                            {result.title}
                          </div>
                          <div className="text-xs text-neutral-500 dark:text-neutral-400 truncate">
                            {result.subtitle}
                          </div>
                        </div>
                        {result.value !== undefined && (
                          <div className="text-sm font-medium text-neutral-700 dark:text-neutral-300">
                            {formatCurrency(result.value)}
                          </div>
                        )}
                        <ArrowRight size={16} className="text-neutral-400" />
                      </button>
                    </li>
                  );
                })}
              </ul>
            ) : query.trim() ? (
              <div className="py-8 text-center text-neutral-500">No results found</div>
            ) : (
              <div className="py-6 px-4 text-center">
                <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-3">
                  Search across all your financial data
                </p>
                <div className="flex items-center justify-center gap-2 text-xs text-neutral-400">
                  <kbd className="px-2 py-1 bg-neutral-100 dark:bg-neutral-700 rounded text-neutral-600 dark:text-neutral-300">
                    Esc
                  </kbd>
                  <span>to close</span>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};
