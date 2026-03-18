import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Cloud, HardDrive, DollarSign, Shield, Wifi, WifiOff } from 'lucide-react';
import { useAuth } from '../utils/auth-context';

export const LandingPage: React.FC = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  if (isAuthenticated) {
    navigate('/', { replace: true });
    return null;
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-neutral-50 to-neutral-100 dark:from-neutral-950 dark:to-neutral-900 flex items-center justify-center p-4">
      <div className="w-full max-w-2xl">
        <div className="text-center mb-10">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-primary-600 text-white mb-4">
            <DollarSign size={32} />
          </div>
          <h1 className="text-3xl font-bold text-neutral-900 dark:text-white mb-2">Welcome to Finora</h1>
          <p className="text-neutral-500 dark:text-neutral-400">Your personal finance tracker. Choose how you want to use it.</p>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <button
            onClick={() => navigate('/login')}
            className="group relative bg-white dark:bg-neutral-900 rounded-xl border-2 border-neutral-200 dark:border-neutral-800 p-6 text-left hover:border-primary-500 dark:hover:border-primary-500 transition-all duration-200 hover:shadow-lg"
          >
            <div className="flex items-center gap-3 mb-4">
              <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-primary-50 dark:bg-primary-900/30 text-primary-600 dark:text-primary-400">
                <Cloud size={22} />
              </div>
              <div className="flex items-center gap-2">
                <Wifi size={14} className="text-green-500" />
                <span className="text-xs font-medium text-green-600 dark:text-green-400 uppercase tracking-wider">Online</span>
              </div>
            </div>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">Cloud Mode</h2>
            <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-4">
              Sign in to sync your data across devices. Requires a server connection.
            </p>
            <ul className="space-y-1.5 text-xs text-neutral-500 dark:text-neutral-400">
              <li className="flex items-center gap-2"><span className="text-green-500">&#10003;</span> Sync across devices</li>
              <li className="flex items-center gap-2"><span className="text-green-500">&#10003;</span> Live price updates</li>
              <li className="flex items-center gap-2"><span className="text-green-500">&#10003;</span> Statement import</li>
              <li className="flex items-center gap-2"><span className="text-green-500">&#10003;</span> Encrypted backups</li>
            </ul>
          </button>

          <button
            onClick={() => navigate('/vault')}
            className="group relative bg-white dark:bg-neutral-900 rounded-xl border-2 border-neutral-200 dark:border-neutral-800 p-6 text-left hover:border-emerald-500 dark:hover:border-emerald-500 transition-all duration-200 hover:shadow-lg"
          >
            <div className="flex items-center gap-3 mb-4">
              <div className="flex items-center justify-center w-10 h-10 rounded-lg bg-emerald-50 dark:bg-emerald-900/30 text-emerald-600 dark:text-emerald-400">
                <HardDrive size={22} />
              </div>
              <div className="flex items-center gap-2">
                <WifiOff size={14} className="text-neutral-400" />
                <span className="text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Offline</span>
              </div>
            </div>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white mb-2">Local Vault</h2>
            <p className="text-sm text-neutral-500 dark:text-neutral-400 mb-4">
              No account needed. Your data stays in an encrypted file on your device.
            </p>
            <ul className="space-y-1.5 text-xs text-neutral-500 dark:text-neutral-400">
              <li className="flex items-center gap-2"><span className="text-emerald-500">&#10003;</span> No account required</li>
              <li className="flex items-center gap-2"><span className="text-emerald-500">&#10003;</span> Works fully offline</li>
              <li className="flex items-center gap-2"><span className="text-emerald-500">&#10003;</span> AES-256 encrypted vault</li>
              <li className="flex items-center gap-2"><Shield size={12} className="text-emerald-500" /> Zero-knowledge encryption</li>
            </ul>
          </button>
        </div>

        <p className="text-center text-xs text-neutral-400 dark:text-neutral-600 mt-8">
          You can switch modes anytime. Cloud backups can be opened as local vaults.
        </p>
      </div>
    </div>
  );
};
