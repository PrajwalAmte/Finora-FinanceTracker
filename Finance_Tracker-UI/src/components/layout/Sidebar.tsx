import React from 'react';
import { NavLink } from 'react-router-dom';
import { Home, TrendingUp, DollarSign, CreditCard, BarChart4, X, Menu } from 'lucide-react';
import { ThemeToggle } from './ThemeToggle';

interface SidebarProps {
  isMobileOpen: boolean;
  onMobileClose: () => void;
}

interface NavItem {
  name: string;
  path: string;
  icon: React.ReactNode;
}

export const Sidebar: React.FC<SidebarProps> = ({ isMobileOpen, onMobileClose }) => {
  const navItems: NavItem[] = [
    { name: 'Dashboard', path: '/', icon: <Home size={20} /> },
    { name: 'Investments', path: '/investments', icon: <TrendingUp size={20} /> },
    { name: 'SIPs', path: '/sips', icon: <BarChart4 size={20} /> },
    { name: 'Loans', path: '/loans', icon: <CreditCard size={20} /> },
    { name: 'Expenses', path: '/expenses', icon: <DollarSign size={20} /> },
  ];

  return (
    <>
      {/* Mobile sidebar overlay */}
      {isMobileOpen && (
        <div 
          className="fixed inset-0 bg-neutral-900 bg-opacity-50 z-20 md:hidden"
          onClick={onMobileClose}
        ></div>
      )}
      
      {/* Sidebar */}
      <aside 
        className={`fixed inset-y-0 left-0 z-30 w-64 bg-white dark:bg-neutral-900 border-r border-neutral-200 dark:border-neutral-800 flex flex-col transition-transform duration-300 transform
                    md:translate-x-0 ${isMobileOpen ? 'translate-x-0' : '-translate-x-full'}`}
      >
        {/* Logo and mobile close button */}
        <div className="flex items-center justify-between p-4 border-b border-neutral-200 dark:border-neutral-800">
          <div className="flex items-center space-x-2">
            <DollarSign className="text-primary-600 dark:text-primary-500" />
            <h1 className="text-xl font-bold text-neutral-900 dark:text-white">Finora</h1>
          </div>
          <button 
            className="md:hidden p-2 rounded-md text-neutral-500 hover:text-neutral-700 hover:bg-neutral-100 dark:text-neutral-400 dark:hover:text-neutral-200 dark:hover:bg-neutral-800"
            onClick={onMobileClose}
          >
            <X size={20} />
          </button>
        </div>
        
        {/* Navigation links */}
        <nav className="flex-1 overflow-y-auto p-4">
          <ul className="space-y-1">
            {navItems.map((item) => (
              <li key={item.path}>
                <NavLink
                  to={item.path}
                  className={({ isActive }) =>
                    `flex items-center px-4 py-3 text-sm font-medium rounded-md transition-colors ${
                      isActive
                        ? 'bg-primary-50 text-primary-700 dark:bg-primary-900/30 dark:text-primary-400'
                        : 'text-neutral-700 hover:bg-neutral-100 dark:text-neutral-300 dark:hover:bg-neutral-800'
                    }`
                  }
                  onClick={() => onMobileClose()}
                >
                  <span className="mr-3">{item.icon}</span>
                  {item.name}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>
        
        <div className="p-4 border-t border-neutral-200 dark:border-neutral-800 flex justify-between items-center">
          <p className="text-sm text-neutral-500 dark:text-neutral-400">© 2025 Finora</p>
          <ThemeToggle />
        </div>
      </aside>
    </>
  );
};