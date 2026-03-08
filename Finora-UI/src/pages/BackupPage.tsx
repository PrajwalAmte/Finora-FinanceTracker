import React, { useState, useRef, useCallback } from 'react';
import {
  Download,
  Upload,
  Shield,
  Lock,
  FileDown,
  FileUp,
  AlertTriangle,
  CheckCircle2,
  Eye,
  EyeOff,
  HardDrive,
  Info,
} from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Badge } from '../components/ui/Badge';
import { backupApi, BackupMetadata } from '../api/backupApi';
import { toast } from '../utils/notifications';
import { formatDate } from '../utils/formatters';

// ── Export Section ─────────────────────────────────────────────────────

const ExportSection: React.FC = () => {
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [isExporting, setIsExporting] = useState(false);

  const passwordsMatch = password === confirmPassword;
  const isValid = password.length >= 8 && passwordsMatch;

  const handleExport = useCallback(async () => {
    if (!isValid) return;

    setIsExporting(true);
    try {
      const blob = await backupApi.exportBackup(password);

      // Trigger browser download
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `finora-backup-${new Date().toISOString().slice(0, 10)}.enc`;
      document.body.appendChild(link);
      link.click();

      // Cleanup
      setTimeout(() => {
        URL.revokeObjectURL(url);
        document.body.removeChild(link);
      }, 100);

      toast.success('Backup exported successfully');
      setPassword('');
      setConfirmPassword('');
    } catch (error: any) {
      toast.error(error?.message || 'Failed to export backup');
    } finally {
      setIsExporting(false);
    }
  }, [password, isValid]);

  return (
    <Card
      title="Export Backup"
      subtitle="Download an encrypted copy of all your financial data"
      icon={<FileDown size={18} />}
    >
      <div className="space-y-4">
        {/* Info banner */}
        <div className="flex items-start gap-3 p-3 rounded-lg bg-primary-50 dark:bg-primary-900/20 border border-primary-100 dark:border-primary-800">
          <Info size={18} className="text-primary-600 dark:text-primary-400 mt-0.5 shrink-0" />
          <p className="text-sm text-primary-700 dark:text-primary-300">
            Your backup includes all expenses, investments, loans, SIPs, and the full ledger
            audit trail. It is encrypted with AES-256-GCM — only you can decrypt it with your
            password.
          </p>
        </div>

        {/* Password inputs */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div className="relative">
            <Input
              label="Encryption Password"
              type={showPassword ? 'text' : 'password'}
              placeholder="Minimum 8 characters"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              fullWidth
              icon={<Lock size={16} />}
              error={password.length > 0 && password.length < 8 ? 'At least 8 characters' : undefined}
            />
          </div>

          <div className="relative">
            <Input
              label="Confirm Password"
              type={showPassword ? 'text' : 'password'}
              placeholder="Re-enter password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              fullWidth
              icon={<Lock size={16} />}
              error={
                confirmPassword.length > 0 && !passwordsMatch
                  ? 'Passwords do not match'
                  : undefined
              }
            />
          </div>
        </div>

        <div className="flex items-center justify-between">
          <button
            type="button"
            onClick={() => setShowPassword((prev) => !prev)}
            className="flex items-center gap-1.5 text-xs text-neutral-500 hover:text-neutral-700 dark:text-neutral-400 dark:hover:text-neutral-200 transition-colors"
          >
            {showPassword ? <EyeOff size={14} /> : <Eye size={14} />}
            {showPassword ? 'Hide passwords' : 'Show passwords'}
          </button>

          <Button
            variant="primary"
            iconLeft={<Download size={16} />}
            onClick={handleExport}
            disabled={!isValid}
            isLoading={isExporting}
          >
            Export Encrypted Backup
          </Button>
        </div>
      </div>
    </Card>
  );
};

// ── Import Section ─────────────────────────────────────────────────────

const ImportSection: React.FC = () => {
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isImporting, setIsImporting] = useState(false);
  const [importResult, setImportResult] = useState<BackupMetadata | null>(null);
  const [showConfirm, setShowConfirm] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const isValid = password.length >= 8 && selectedFile !== null;

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    if (file) {
      // Basic validation
      if (file.size > 50 * 1024 * 1024) {
        toast.error('File exceeds 50 MB limit');
        return;
      }
      setSelectedFile(file);
      setImportResult(null);
    }
  };

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    const file = e.dataTransfer.files[0];
    if (file) {
      if (file.size > 50 * 1024 * 1024) {
        toast.error('File exceeds 50 MB limit');
        return;
      }
      setSelectedFile(file);
      setImportResult(null);
    }
  }, []);

  const handleDragOver = useCallback((e: React.DragEvent) => {
    e.preventDefault();
  }, []);

  const handleImport = useCallback(async () => {
    if (!isValid || !selectedFile) return;

    setIsImporting(true);
    setShowConfirm(false);
    try {
      const metadata = await backupApi.importBackup(selectedFile, password);
      setImportResult(metadata);
      toast.success('Backup imported successfully — all data restored');
      setPassword('');
      setSelectedFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
    } catch (error: any) {
      const msg =
        error?.response?.data?.message || error?.message || 'Failed to import backup';
      toast.error(msg);
    } finally {
      setIsImporting(false);
    }
  }, [password, selectedFile, isValid]);

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(2)} MB`;
  };

  return (
    <Card
      title="Import Backup"
      subtitle="Restore your data from an encrypted backup file"
      icon={<FileUp size={18} />}
    >
      <div className="space-y-4">
        {/* Warning banner */}
        <div className="flex items-start gap-3 p-3 rounded-lg bg-warning-50 dark:bg-warning-900/20 border border-warning-200 dark:border-warning-800">
          <AlertTriangle
            size={18}
            className="text-warning-600 dark:text-warning-400 mt-0.5 shrink-0"
          />
          <p className="text-sm text-warning-700 dark:text-warning-300">
            Importing a backup will <strong>replace all your current data</strong> (expenses,
            investments, loans, SIPs, and ledger events). This action cannot be undone.
          </p>
        </div>

        {/* Drag & Drop zone */}
        <div
          onDrop={handleDrop}
          onDragOver={handleDragOver}
          onClick={() => fileInputRef.current?.click()}
          className="flex flex-col items-center justify-center gap-2 p-6 border-2 border-dashed border-neutral-300 dark:border-neutral-600 rounded-lg cursor-pointer hover:border-primary-400 dark:hover:border-primary-500 hover:bg-neutral-50 dark:hover:bg-neutral-800/50 transition-colors"
        >
          <Upload size={28} className="text-neutral-400 dark:text-neutral-500" />
          {selectedFile ? (
            <div className="text-center">
              <p className="text-sm font-medium text-neutral-900 dark:text-neutral-100">
                {selectedFile.name}
              </p>
              <p className="text-xs text-neutral-500 dark:text-neutral-400">
                {formatFileSize(selectedFile.size)}
              </p>
            </div>
          ) : (
            <div className="text-center">
              <p className="text-sm text-neutral-600 dark:text-neutral-300">
                Drag & drop your <code className="text-xs bg-neutral-100 dark:bg-neutral-700 px-1 py-0.5 rounded">.enc</code> backup file here
              </p>
              <p className="text-xs text-neutral-400 dark:text-neutral-500 mt-1">
                or click to browse · max 50 MB
              </p>
            </div>
          )}
          <input
            ref={fileInputRef}
            type="file"
            accept=".enc,application/octet-stream"
            onChange={handleFileSelect}
            className="hidden"
          />
        </div>

        {/* Password */}
        <div className="relative max-w-sm">
          <Input
            label="Decryption Password"
            type={showPassword ? 'text' : 'password'}
            placeholder="Enter the password used during export"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            fullWidth
            icon={<Lock size={16} />}
            error={password.length > 0 && password.length < 8 ? 'At least 8 characters' : undefined}
          />
        </div>

        <div className="flex items-center justify-between">
          <button
            type="button"
            onClick={() => setShowPassword((prev) => !prev)}
            className="flex items-center gap-1.5 text-xs text-neutral-500 hover:text-neutral-700 dark:text-neutral-400 dark:hover:text-neutral-200 transition-colors"
          >
            {showPassword ? <EyeOff size={14} /> : <Eye size={14} />}
            {showPassword ? 'Hide password' : 'Show password'}
          </button>

          {!showConfirm ? (
            <Button
              variant="danger"
              iconLeft={<Upload size={16} />}
              onClick={() => setShowConfirm(true)}
              disabled={!isValid}
              isLoading={isImporting}
            >
              Import Backup
            </Button>
          ) : (
            <div className="flex items-center gap-2">
              <span className="text-sm text-warning-600 dark:text-warning-400 font-medium">
                Replace all data?
              </span>
              <Button variant="outline" size="sm" onClick={() => setShowConfirm(false)}>
                Cancel
              </Button>
              <Button
                variant="danger"
                size="sm"
                onClick={handleImport}
                isLoading={isImporting}
              >
                Yes, Import
              </Button>
            </div>
          )}
        </div>

        {/* Import result */}
        {importResult && (
          <div className="mt-4 p-4 rounded-lg bg-success-50 dark:bg-success-900/20 border border-success-200 dark:border-success-800">
            <div className="flex items-center gap-2 mb-3">
              <CheckCircle2 size={18} className="text-success-600 dark:text-success-400" />
              <span className="text-sm font-semibold text-success-700 dark:text-success-300">
                Backup Restored Successfully
              </span>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 text-sm">
              <MetadataPill label="Expenses" value={importResult.expenseCount} />
              <MetadataPill label="Investments" value={importResult.investmentCount} />
              <MetadataPill label="Loans" value={importResult.loanCount} />
              <MetadataPill label="SIPs" value={importResult.sipCount} />
              <MetadataPill label="Ledger Events" value={importResult.ledgerEventCount} />
              <MetadataPill
                label="Exported"
                value={formatDate(importResult.exportTimestamp)}
              />
            </div>
          </div>
        )}
      </div>
    </Card>
  );
};

// ── Metadata pill ──────────────────────────────────────────────────────

const MetadataPill: React.FC<{ label: string; value: string | number }> = ({ label, value }) => (
  <div className="flex items-center gap-1.5">
    <span className="text-neutral-500 dark:text-neutral-400">{label}:</span>
    <Badge variant="default" size="sm">
      {value}
    </Badge>
  </div>
);

// ── Main Page ──────────────────────────────────────────────────────────

export const BackupPage: React.FC = () => {
  return (
    <div className="max-w-3xl space-y-6 animate-fade-in">
      {/* Page intro */}
      <Card>
        <div className="flex items-start gap-4">
          <div className="w-12 h-12 rounded-full bg-primary-100 dark:bg-primary-900/40 flex items-center justify-center shrink-0">
            <HardDrive size={22} className="text-primary-700 dark:text-primary-300" />
          </div>
          <div>
            <h2 className="text-lg font-semibold text-neutral-900 dark:text-white">
              Encrypted Backups
            </h2>
            <p className="text-sm text-neutral-500 dark:text-neutral-400 mt-1">
              Export and import encrypted snapshots of your complete financial data, including the
              full ledger audit trail with hash-chain integrity verification.
            </p>
            <div className="flex flex-wrap gap-2 mt-3">
              <Badge variant="primary" size="sm">
                <Shield size={12} className="mr-1" /> AES-256-GCM
              </Badge>
              <Badge variant="primary" size="sm">
                <Lock size={12} className="mr-1" /> PBKDF2 Key Derivation
              </Badge>
              <Badge variant="success" size="sm">
                <CheckCircle2 size={12} className="mr-1" /> Ledger Integrity Verified
              </Badge>
            </div>
          </div>
        </div>
      </Card>

      <ExportSection />
      <ImportSection />
    </div>
  );
};
