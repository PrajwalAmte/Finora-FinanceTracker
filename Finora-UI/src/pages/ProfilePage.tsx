import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, Mail, Calendar, Clock, LogOut, Shield, Lock, Unlock, AlertTriangle } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Dialog } from '../components/ui/Dialog';
import { Badge } from '../components/ui/Badge';
import { useAuth } from '../utils/auth-context';
import { toast } from '../utils/notifications';
import { formatDate } from '../utils/formatters';
import { vaultApi } from '../api/vaultApi';
import { deriveVaultKey } from '../utils/vault-crypto';

const VAULT_CONFIRM_TEXT = 'I understand I will permanently lose all data if I lose this passphrase';

function InfoRow({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: React.ReactNode;
}) {
  return (
    <div className="flex items-start py-3 border-b border-neutral-100 dark:border-neutral-700 last:border-0">
      <div className="flex items-center w-40 shrink-0 text-neutral-500 dark:text-neutral-400 text-sm">
        <span className="mr-2">{icon}</span>
        {label}
      </div>
      <div className="text-sm text-neutral-900 dark:text-neutral-100 font-medium">{value}</div>
    </div>
  );
}

export const ProfilePage: React.FC = () => {
  const navigate = useNavigate();
  const { user, logout, vaultUnlocked, unlockVault, lockVault, refreshUser } = useAuth();

  // Vault modal states
  const [showEnableModal, setShowEnableModal] = useState(false);
  const [showUnlockModal, setShowUnlockModal] = useState(false);
  const [showDisableModal, setShowDisableModal] = useState(false);

  // Form states
  const [passphrase, setPassphrase] = useState('');
  const [passphraseConfirm, setPassphraseConfirm] = useState('');
  const [confirmText, setConfirmText] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  const handleLogout = () => {
    logout();
    toast.info('You have been signed out.');
    navigate('/login', { replace: true });
  };

  const resetForms = () => {
    setPassphrase('');
    setPassphraseConfirm('');
    setConfirmText('');
  };

  const handleEnableVault = async () => {
    if (passphrase.length < 8) {
      toast.error('Passphrase must be at least 8 characters');
      return;
    }
    if (passphrase !== passphraseConfirm) {
      toast.error('Passphrases do not match');
      return;
    }
    if (confirmText !== VAULT_CONFIRM_TEXT) {
      toast.error('Please type the confirmation text exactly');
      return;
    }

    setIsLoading(true);
    try {
      const result = await vaultApi.enable({
        passphrase,
        confirmation: confirmText,
      });

      // Derive and store the vault key
      if (result.vaultSalt) {
        const derivedKey = await deriveVaultKey(passphrase, result.vaultSalt);
        unlockVault(derivedKey);
      }

      await refreshUser();
      toast.success('Vault encryption enabled successfully');
      setShowEnableModal(false);
      resetForms();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to enable vault';
      toast.error(msg);
    } finally {
      setIsLoading(false);
    }
  };

  const handleUnlockVault = async () => {
    if (!passphrase || !user?.vaultSalt) {
      toast.error('Please enter your passphrase');
      return;
    }

    setIsLoading(true);
    try {
      const derivedKey = await deriveVaultKey(passphrase, user.vaultSalt);
      unlockVault(derivedKey);
      toast.success('Vault unlocked');
      setShowUnlockModal(false);
      resetForms();
    } catch {
      toast.error('Failed to derive vault key');
    } finally {
      setIsLoading(false);
    }
  };

  const handleLockVault = () => {
    lockVault();
    toast.info('Vault locked');
  };

  const handleDisableVault = async () => {
    if (!passphrase) {
      toast.error('Please enter your passphrase');
      return;
    }

    setIsLoading(true);
    try {
      await vaultApi.disable({ passphrase });
      lockVault();
      await refreshUser();
      toast.success('Vault encryption disabled');
      setShowDisableModal(false);
      resetForms();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Failed to disable vault';
      toast.error(msg);
    } finally {
      setIsLoading(false);
    }
  };

  if (!user) return null;

  const initials = user.username.slice(0, 2).toUpperCase();

  return (
    <div className="max-w-2xl space-y-6">
      {/* Avatar + name header */}
      <Card>
        <div className="flex items-center space-x-5">
          <div className="w-16 h-16 rounded-full bg-primary-100 dark:bg-primary-900/40 flex items-center justify-center shrink-0">
            <span className="text-xl font-bold text-primary-700 dark:text-primary-300">
              {initials}
            </span>
          </div>
          <div className="flex-1 min-w-0">
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white truncate">
              {user.username}
            </h2>
            <p className="text-sm text-neutral-500 dark:text-neutral-400 truncate">{user.email}</p>
          </div>
        </div>
      </Card>

      {/* Account details */}
      <Card title="Account Details" icon={<User size={18} />}>
        <div>
          <InfoRow icon={<User size={15} />} label="Username" value={user.username} />
          <InfoRow icon={<Mail size={15} />} label="Email" value={user.email} />
        </div>
      </Card>

      {/* Vault Encryption */}
      <Card title="Vault Encryption" icon={<Shield size={18} />}>
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-sm text-neutral-700 dark:text-neutral-300">
                Extra encryption layer with your personal passphrase
              </p>
              <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1">
                {user.vaultEnabled
                  ? 'Your data is protected with vault encryption'
                  : 'Add an additional layer of security to your sensitive data'}
              </p>
            </div>
            <Badge variant={user.vaultEnabled ? 'success' : 'neutral'} size="sm">
              {user.vaultEnabled ? 'Enabled' : 'Disabled'}
            </Badge>
          </div>

          {user.vaultEnabled ? (
            <div className="flex items-center gap-3">
              {vaultUnlocked ? (
                <>
                  <Badge variant="success" size="sm">
                    <Unlock size={12} className="mr-1" /> Unlocked
                  </Badge>
                  <Button
                    variant="secondary"
                    size="sm"
                    iconLeft={<Lock size={14} />}
                    onClick={handleLockVault}
                  >
                    Lock Vault
                  </Button>
                </>
              ) : (
                <Button
                  variant="primary"
                  size="sm"
                  iconLeft={<Unlock size={14} />}
                  onClick={() => setShowUnlockModal(true)}
                >
                  Unlock Vault
                </Button>
              )}
              <Button
                variant="danger"
                size="sm"
                onClick={() => setShowDisableModal(true)}
              >
                Disable Vault
              </Button>
            </div>
          ) : (
            <Button
              variant="primary"
              size="sm"
              iconLeft={<Shield size={14} />}
              onClick={() => setShowEnableModal(true)}
            >
              Enable Vault Encryption
            </Button>
          )}
        </div>
      </Card>

      {/* Activity */}
      <Card title="Activity" icon={<Clock size={18} />}>
        <div>
          <InfoRow
            icon={<Calendar size={15} />}
            label="Member since"
            value={formatDate(user.createdAt)}
          />
          <InfoRow
            icon={<Clock size={15} />}
            label="Last updated"
            value={formatDate(user.updatedAt)}
          />
          <InfoRow
            icon={<Clock size={15} />}
            label="Last login"
            value={user.lastLoginAt ? formatDate(user.lastLoginAt) : '—'}
          />
        </div>
      </Card>

      {/* Logout */}
      <div>
        <Button variant="danger" iconLeft={<LogOut size={16} />} onClick={handleLogout}>
          Sign out
        </Button>
      </div>

      {/* Enable Vault Modal */}
      <Dialog
        isOpen={showEnableModal}
        onClose={() => {
          setShowEnableModal(false);
          resetForms();
        }}
        title="Enable Vault Encryption"
      >
        <div className="space-y-4">
          <div className="p-3 bg-amber-50 dark:bg-amber-900/30 border border-amber-200 dark:border-amber-700 rounded-lg">
            <div className="flex items-start gap-2">
              <AlertTriangle className="text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" size={18} />
              <div className="text-sm text-amber-800 dark:text-amber-200">
                <p className="font-semibold">Warning: No recovery option</p>
                <p className="mt-1">
                  If you lose your passphrase, your encrypted data cannot be recovered.
                  There is no password reset or recovery mechanism.
                </p>
              </div>
            </div>
          </div>

          <Input
            type="password"
            label="Passphrase (min 8 characters)"
            value={passphrase}
            onChange={(e) => setPassphrase(e.target.value)}
            placeholder="Enter a strong passphrase"
            fullWidth
          />

          <Input
            type="password"
            label="Confirm Passphrase"
            value={passphraseConfirm}
            onChange={(e) => setPassphraseConfirm(e.target.value)}
            placeholder="Enter passphrase again"
            fullWidth
          />

          <div>
            <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
              Type the following to confirm:
            </label>
            <p className="text-xs text-neutral-500 dark:text-neutral-400 mb-2 italic">
              "{VAULT_CONFIRM_TEXT}"
            </p>
            <Input
              type="text"
              value={confirmText}
              onChange={(e) => setConfirmText(e.target.value)}
              placeholder="Type confirmation text"
              fullWidth
            />
          </div>

          <div className="flex justify-end gap-3 pt-2">
            <Button
              variant="secondary"
              onClick={() => {
                setShowEnableModal(false);
                resetForms();
              }}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleEnableVault}
              isLoading={isLoading}
              disabled={
                passphrase.length < 8 ||
                passphrase !== passphraseConfirm ||
                confirmText !== VAULT_CONFIRM_TEXT
              }
            >
              Enable Vault
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Unlock Vault Modal */}
      <Dialog
        isOpen={showUnlockModal}
        onClose={() => {
          setShowUnlockModal(false);
          resetForms();
        }}
        title="Unlock Vault"
      >
        <div className="space-y-4">
          <p className="text-sm text-neutral-600 dark:text-neutral-400">
            Enter your vault passphrase to decrypt your data.
          </p>

          <Input
            type="password"
            label="Passphrase"
            value={passphrase}
            onChange={(e) => setPassphrase(e.target.value)}
            placeholder="Enter your vault passphrase"
            fullWidth
            autoFocus
          />

          <div className="flex justify-end gap-3 pt-2">
            <Button
              variant="secondary"
              onClick={() => {
                setShowUnlockModal(false);
                resetForms();
              }}
            >
              Cancel
            </Button>
            <Button
              variant="primary"
              onClick={handleUnlockVault}
              isLoading={isLoading}
              disabled={!passphrase}
            >
              Unlock
            </Button>
          </div>
        </div>
      </Dialog>

      {/* Disable Vault Modal */}
      <Dialog
        isOpen={showDisableModal}
        onClose={() => {
          setShowDisableModal(false);
          resetForms();
        }}
        title="Disable Vault Encryption"
      >
        <div className="space-y-4">
          <div className="p-3 bg-red-50 dark:bg-red-900/30 border border-red-200 dark:border-red-700 rounded-lg">
            <p className="text-sm text-red-800 dark:text-red-200">
              This will remove the extra encryption layer. Your data will still be protected
              by server-side encryption.
            </p>
          </div>

          <Input
            type="password"
            label="Current Passphrase"
            value={passphrase}
            onChange={(e) => setPassphrase(e.target.value)}
            placeholder="Enter your vault passphrase"
            fullWidth
          />

          <div className="flex justify-end gap-3 pt-2">
            <Button
              variant="secondary"
              onClick={() => {
                setShowDisableModal(false);
                resetForms();
              }}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              onClick={handleDisableVault}
              isLoading={isLoading}
              disabled={!passphrase}
            >
              Disable Vault
            </Button>
          </div>
        </div>
      </Dialog>
    </div>
  );
};
