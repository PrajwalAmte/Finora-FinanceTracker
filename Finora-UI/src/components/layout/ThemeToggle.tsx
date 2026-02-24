import React from 'react';
import { Sun, Moon } from 'lucide-react';
import { useTheme } from '../../utils/theme-context';

export const ThemeToggle: React.FC = () => {
  const { theme, toggleTheme } = useTheme();
  
  return (
    <button
      onClick={toggleTheme}
      className="p-2 rounded-full text-neutral-600 hover:bg-neutral-100 focus:outline-none focus:ring-2 focus:ring-primary-500 transition-colors
                 dark:text-neutral-400 dark:hover:bg-neutral-800"
      aria-label={theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode'}
    >
      {theme === 'dark' ? (
        <Sun size={20} className="animate-scale-in" />
      ) : (
        <Moon size={20} className="animate-scale-in" />
      )}
    </button>
  );
};