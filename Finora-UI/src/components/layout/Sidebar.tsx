import React from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { Home, TrendingUp, DollarSign, CreditCard, BarChart4, X, User, LogOut, HardDrive, Save } from 'lucide-react';
import { ThemeToggle } from './ThemeToggle';
import { useAuth } from '../../utils/auth-context';
import { useLocalVault } from '../../utils/local-vault-context';
import { toast } from '../../utils/notifications';

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
  const navigate = useNavigate();
  const { user, logout } = useAuth();
  const vault = useLocalVault();

  const baseNavItems: NavItem[] = [
    { name: 'Dashboard', path: '/', icon: <Home size={20} /> },
    { name: 'Investments', path: '/investments', icon: <TrendingUp size={20} /> },
    { name: 'SIPs', path: '/sips', icon: <BarChart4 size={20} /> },
    { name: 'Loans', path: '/loans', icon: <CreditCard size={20} /> },
    { name: 'Expenses', path: '/expenses', icon: <DollarSign size={20} /> },
  ];

  const cloudNavItems: NavItem[] = [
    { name: 'Profile', path: '/profile', icon: <User size={20} /> },
    { name: 'Backup', path: '/backup', icon: <HardDrive size={20} /> },
  ];

  const navItems = vault.isLocalMode ? baseNavItems : [...baseNavItems, ...cloudNavItems];

  const handleLogout = () => {
    if (vault.isLocalMode) {
      if (vault.isDirty && !window.confirm('You have unsaved changes. Close vault anyway?')) return;
      vault.closeVault();
      toast.info('Vault closed.');
      navigate('/welcome', { replace: true });
    } else {
      logout();
      toast.info('Signed out successfully.');
      navigate('/welcome', { replace: true });
    }
  };

  const handleSaveVault = async () => {
    try {
      await vault.saveVaultToFile();
      toast.success('Vault saved');
    } catch {
      toast.error('Failed to save vault');
    }
  };

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
        
        <div className="p-4 border-t border-neutral-200 dark:border-neutral-800 space-y-3">
          {vault.isLocalMode ? (
            <>
              <button
                onClick={handleSaveVault}
                className="w-full flex items-center justify-center gap-2 py-2 px-3 rounded-lg text-sm font-medium bg-emerald-50 dark:bg-emerald-900/30 text-emerald-700 dark:text-emerald-300 hover:bg-emerald-100 dark:hover:bg-emerald-900/50 transition-colors"
              >
                <Save size={16} />
                Save Vault
                {vault.isDirty && <span className="w-2 h-2 rounded-full bg-amber-500 animate-pulse" />}
              </button>
              <div className="flex items-center justify-center">
                <span className="text-xs text-neutral-500 dark:text-neutral-400">Local Vault Mode</span>
              </div>
            </>
          ) : user && (
            <div className="flex items-center space-x-3">
              <div className="w-8 h-8 rounded-full bg-primary-100 dark:bg-primary-900/40 flex items-center justify-center shrink-0">
                <span className="text-xs font-bold text-primary-700 dark:text-primary-300">
                  {user.username.slice(0, 2).toUpperCase()}
                </span>
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-neutral-900 dark:text-white truncate">
                  {user.username}
                </p>
                <p className="text-xs text-neutral-500 dark:text-neutral-400 truncate">
                  {user.email}
                </p>
              </div>
            </div>
          )}

          <div className="flex items-center justify-between">
            <button
              onClick={handleLogout}
              className="flex items-center text-sm text-neutral-500 hover:text-error-600 dark:text-neutral-400 dark:hover:text-error-400 transition-colors"
            >
              <LogOut size={15} className="mr-1.5" />
              {vault.isLocalMode ? 'Close Vault' : 'Sign out'}
            </button>
            <ThemeToggle />
          </div>
        </div>
      </aside>
    </>
  );
};