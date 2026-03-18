import React, { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { DollarSign, FolderOpen, Plus, Lock, Eye, EyeOff, ArrowLeft } from 'lucide-react';
import { useLocalVault } from '../utils/local-vault-context';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { toast } from '../utils/notifications';

type Tab = 'open' | 'create';

export const LocalVaultPage: React.FC = () => {
  const navigate = useNavigate();
  const vault = useLocalVault();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [tab, setTab] = useState<Tab>('open');
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [passphrase, setPassphrase] = useState('');
  const [confirmPassphrase, setConfirmPassphrase] = useState('');
  const [showPass, setShowPass] = useState(false);
  const [isLoading, setIsLoading] = useState(false);

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    setSelectedFile(file);
  };

  const handleOpen = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedFile || !passphrase) return;
    try {
      setIsLoading(true);
      await vault.openVaultFromFile(selectedFile, passphrase);
      toast.success('Vault opened successfully');
      navigate('/', { replace: true });
    } catch {
      toast.error('Failed to open vault. Check your passphrase and try again.');
    } finally {
      setIsLoading(false);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!passphrase) return;
    if (passphrase.length < 8) {
      toast.error('Passphrase must be at least 8 characters');
      return;
    }
    if (passphrase !== confirmPassphrase) {
      toast.error('Passphrases do not match');
      return;
    }
    try {
      setIsLoading(true);
      vault.createNewVault(passphrase);
      toast.success('New vault created');
      navigate('/', { replace: true });
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-neutral-50 to-neutral-100 dark:from-neutral-950 dark:to-neutral-900 flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        <button
          onClick={() => navigate('/welcome')}
          className="flex items-center gap-1.5 text-sm text-neutral-500 hover:text-neutral-700 dark:text-neutral-400 dark:hover:text-neutral-200 mb-6 transition-colors"
        >
          <ArrowLeft size={16} />
          Back
        </button>

        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-emerald-600 text-white mb-3">
            <DollarSign size={28} />
          </div>
          <h1 className="text-2xl font-bold text-neutral-900 dark:text-white">Local Vault</h1>
          <p className="text-sm text-neutral-500 dark:text-neutral-400 mt-1">Encrypted file, fully offline</p>
        </div>

        <div className="bg-white dark:bg-neutral-900 rounded-xl border border-neutral-200 dark:border-neutral-800 overflow-hidden">
          <div className="flex border-b border-neutral-200 dark:border-neutral-800">
            <button
              onClick={() => setTab('open')}
              className={`flex-1 py-3 text-sm font-medium transition-colors ${
                tab === 'open'
                  ? 'text-emerald-600 dark:text-emerald-400 border-b-2 border-emerald-600 dark:border-emerald-400'
                  : 'text-neutral-500 dark:text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-200'
              }`}
            >
              <FolderOpen size={16} className="inline mr-1.5 -mt-0.5" />
              Open Existing
            </button>
            <button
              onClick={() => setTab('create')}
              className={`flex-1 py-3 text-sm font-medium transition-colors ${
                tab === 'create'
                  ? 'text-emerald-600 dark:text-emerald-400 border-b-2 border-emerald-600 dark:border-emerald-400'
                  : 'text-neutral-500 dark:text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-200'
              }`}
            >
              <Plus size={16} className="inline mr-1.5 -mt-0.5" />
              Create New
            </button>
          </div>

          <div className="p-6">
            {tab === 'open' ? (
              <form onSubmit={handleOpen} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1.5">
                    Vault File
                  </label>
                  <input
                    ref={fileInputRef}
                    type="file"
                    accept=".enc"
                    onChange={handleFileSelect}
                    className="hidden"
                  />
                  <button
                    type="button"
                    onClick={() => fileInputRef.current?.click()}
                    className="w-full flex items-center justify-center gap-2 py-3 px-4 rounded-lg border-2 border-dashed border-neutral-300 dark:border-neutral-700 hover:border-emerald-400 dark:hover:border-emerald-600 text-sm text-neutral-600 dark:text-neutral-400 transition-colors"
                  >
                    <FolderOpen size={18} />
                    {selectedFile ? selectedFile.name : 'Choose .enc vault file'}
                  </button>
                </div>

                <div className="relative">
                  <Input
                    type={showPass ? 'text' : 'password'}
                    label="Passphrase"
                    value={passphrase}
                    onChange={e => setPassphrase(e.target.value)}
                    placeholder="Enter vault passphrase"
                    required
                    fullWidth
                    icon={<Lock size={16} />}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPass(v => !v)}
                    className="absolute right-3 top-9 text-neutral-400 hover:text-neutral-600 dark:hover:text-neutral-300"
                    tabIndex={-1}
                  >
                    {showPass ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>

                <Button
                  type="submit"
                  fullWidth
                  isLoading={isLoading}
                  disabled={!selectedFile || !passphrase}
                >
                  Unlock Vault
                </Button>
              </form>
            ) : (
              <form onSubmit={handleCreate} className="space-y-4">
                <div className="relative">
                  <Input
                    type={showPass ? 'text' : 'password'}
                    label="Passphrase"
                    value={passphrase}
                    onChange={e => setPassphrase(e.target.value)}
                    placeholder="At least 8 characters"
                    required
                    fullWidth
                    icon={<Lock size={16} />}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPass(v => !v)}
                    className="absolute right-3 top-9 text-neutral-400 hover:text-neutral-600 dark:hover:text-neutral-300"
                    tabIndex={-1}
                  >
                    {showPass ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>

                <Input
                  type={showPass ? 'text' : 'password'}
                  label="Confirm Passphrase"
                  value={confirmPassphrase}
                  onChange={e => setConfirmPassphrase(e.target.value)}
                  placeholder="Re-enter passphrase"
                  required
                  fullWidth
                  icon={<Lock size={16} />}
                />

                {passphrase && passphrase.length < 8 && (
                  <p className="text-xs text-amber-600 dark:text-amber-400">Passphrase must be at least 8 characters</p>
                )}

                <Button
                  type="submit"
                  fullWidth
                  isLoading={isLoading}
                  disabled={!passphrase || passphrase.length < 8 || passphrase !== confirmPassphrase}
                >
                  Create Vault
                </Button>
              </form>
            )}
          </div>
        </div>

        <p className="text-center text-xs text-neutral-400 dark:text-neutral-600 mt-4">
          Your vault is encrypted with AES-256-GCM. We never see your passphrase.
        </p>
      </div>
    </div>
  );
};
