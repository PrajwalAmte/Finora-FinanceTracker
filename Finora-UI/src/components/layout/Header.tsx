import React, { useState } from 'react';
import { Menu, Search } from 'lucide-react';
import { GlobalSearch } from './GlobalSearch';

interface HeaderProps {
  title: string;
  subtitle?: string;
  onMobileMenuClick: () => void;
  actions?: React.ReactNode;
}

export const Header: React.FC<HeaderProps> = ({ 
  title, 
  subtitle, 
  onMobileMenuClick,
  actions
}) => {
  const [isSearchOpen, setIsSearchOpen] = useState(false);

  return (
    <>
      <header className="bg-white dark:bg-neutral-900 border-b border-neutral-200 dark:border-neutral-800 sticky top-0 z-10">
        <div className="px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center">
              <button
                type="button"
                className="md:hidden p-2 mr-3 rounded-md text-neutral-500 hover:text-neutral-700 hover:bg-neutral-100 dark:text-neutral-400 dark:hover:text-neutral-200 dark:hover:bg-neutral-800"
                onClick={onMobileMenuClick}
              >
                <span className="sr-only">Open sidebar</span>
                <Menu size={24} />
              </button>
              
              <div>
                <h1 className="text-xl font-semibold text-neutral-900 dark:text-white">{title}</h1>
                {subtitle && <p className="text-sm text-neutral-500 dark:text-neutral-400">{subtitle}</p>}
              </div>
            </div>

            <div className="flex items-center gap-3">
              <button
                onClick={() => setIsSearchOpen(true)}
                className="flex items-center gap-2 px-3 py-2 text-sm text-neutral-500 dark:text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-200 bg-neutral-100 dark:bg-neutral-800 hover:bg-neutral-200 dark:hover:bg-neutral-700 rounded-lg transition-colors"
              >
                <Search size={16} />
                <span className="hidden sm:inline">Search</span>
                <kbd className="hidden sm:inline-flex items-center px-1.5 py-0.5 text-xs bg-neutral-200 dark:bg-neutral-700 rounded">
                  ⌘K
                </kbd>
              </button>
              {actions && <div className="flex items-center space-x-2">{actions}</div>}
            </div>
          </div>
        </div>
      </header>
      <GlobalSearch isOpen={isSearchOpen} onClose={() => setIsSearchOpen(false)} />
    </>
  );
};