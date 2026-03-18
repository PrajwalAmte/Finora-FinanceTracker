import React, { createContext, useCallback, useContext, useState } from 'react';
import { VaultData, VaultFile } from '../types/LocalVault';
import { Expense } from '../types/Expense';
import { Investment } from '../types/Investment';
import { Loan } from '../types/Loan';
import { Sip } from '../types/Sip';
import { encryptVault, decryptVault } from './local-vault-crypto';

const IDB_NAME = 'finora-local-vault';
const IDB_STORE = 'draft';
const IDB_KEY = 'current';
const VAULT_VERSION = '1.0';

interface LocalVaultContextType {
  isLocalMode: boolean;
  isDirty: boolean;
  data: VaultData;
  passphrase: string | null;
  fileHandle: FileSystemFileHandle | null;
  openVaultFromFile: (file: File, passphrase: string) => Promise<void>;
  createNewVault: (passphrase: string) => void;
  saveVaultToFile: () => Promise<void>;
  downloadVault: () => Promise<void>;
  closeVault: () => void;
  updateExpenses: (updater: (prev: Expense[]) => Expense[]) => void;
  updateInvestments: (updater: (prev: Investment[]) => Investment[]) => void;
  updateLoans: (updater: (prev: Loan[]) => Loan[]) => void;
  updateSips: (updater: (prev: Sip[]) => Sip[]) => void;
}

const emptyData: VaultData = { expenses: [], investments: [], loans: [], sips: [] };

const LocalVaultContext = createContext<LocalVaultContextType | undefined>(undefined);

async function saveDraftToIDB(data: VaultData): Promise<void> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(IDB_NAME, 1);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(IDB_STORE)) {
        db.createObjectStore(IDB_STORE);
      }
    };
    req.onsuccess = () => {
      const tx = req.result.transaction(IDB_STORE, 'readwrite');
      tx.objectStore(IDB_STORE).put(data, IDB_KEY);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    };
    req.onerror = () => reject(req.error);
  });
}

async function loadDraftFromIDB(): Promise<VaultData | null> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(IDB_NAME, 1);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(IDB_STORE)) {
        db.createObjectStore(IDB_STORE);
      }
    };
    req.onsuccess = () => {
      const tx = req.result.transaction(IDB_STORE, 'readonly');
      const getReq = tx.objectStore(IDB_STORE).get(IDB_KEY);
      getReq.onsuccess = () => resolve(getReq.result ?? null);
      getReq.onerror = () => reject(getReq.error);
    };
    req.onerror = () => reject(req.error);
  });
}

async function clearDraftFromIDB(): Promise<void> {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(IDB_NAME, 1);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(IDB_STORE)) {
        db.createObjectStore(IDB_STORE);
      }
    };
    req.onsuccess = () => {
      const tx = req.result.transaction(IDB_STORE, 'readwrite');
      tx.objectStore(IDB_STORE).delete(IDB_KEY);
      tx.oncomplete = () => resolve();
      tx.onerror = () => reject(tx.error);
    };
    req.onerror = () => reject(req.error);
  });
}

function buildVaultFile(data: VaultData): VaultFile {
  const now = new Date().toISOString();
  return { version: VAULT_VERSION, createdAt: now, updatedAt: now, data };
}

export const LocalVaultProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [data, setData] = useState<VaultData>(emptyData);
  const [isLocalMode, setIsLocalMode] = useState(false);
  const [isDirty, setIsDirty] = useState(false);
  const [passphrase, setPassphrase] = useState<string | null>(null);
  const [fileHandle, setFileHandle] = useState<FileSystemFileHandle | null>(null);

  const persistDraft = useCallback((updated: VaultData) => {
    saveDraftToIDB(updated).catch(() => {});
  }, []);

  const openVaultFromFile = useCallback(async (file: File, pass: string) => {
    const buffer = await file.arrayBuffer();
    const json = await decryptVault(buffer, pass);
    const parsed = JSON.parse(json) as VaultFile;

    if (!parsed.data) throw new Error('Invalid vault file format');

    const vaultData: VaultData = {
      expenses: parsed.data.expenses ?? [],
      investments: parsed.data.investments ?? [],
      loans: parsed.data.loans ?? [],
      sips: parsed.data.sips ?? [],
    };

    setData(vaultData);
    setPassphrase(pass);
    setIsLocalMode(true);
    setIsDirty(false);
    await saveDraftToIDB(vaultData);
  }, []);

  const createNewVault = useCallback((pass: string) => {
    setData(emptyData);
    setPassphrase(pass);
    setIsLocalMode(true);
    setIsDirty(false);
    setFileHandle(null);
    saveDraftToIDB(emptyData).catch(() => {});
  }, []);

  const saveVaultToFile = useCallback(async () => {
    if (!passphrase) return;
    const vaultFile = buildVaultFile(data);
    const encrypted = await encryptVault(JSON.stringify(vaultFile), passphrase);

    if (fileHandle) {
      try {
        const writable = await fileHandle.createWritable();
        await writable.write(encrypted);
        await writable.close();
        setIsDirty(false);
        return;
      } catch {
        // File System Access API failed — fall through to download
      }
    }

    if ('showSaveFilePicker' in window) {
      try {
        const handle = await (window as any).showSaveFilePicker({
          suggestedName: `finora-vault-${new Date().toISOString().slice(0, 10)}.enc`,
          types: [{ description: 'Finora Vault', accept: { 'application/octet-stream': ['.enc'] } }],
        });
        const writable = await handle.createWritable();
        await writable.write(encrypted);
        await writable.close();
        setFileHandle(handle);
        setIsDirty(false);
        return;
      } catch (e: any) {
        if (e.name === 'AbortError') return;
      }
    }

    await downloadBlob(encrypted);
    setIsDirty(false);
  }, [passphrase, data, fileHandle]);

  const downloadVault = useCallback(async () => {
    if (!passphrase) return;
    const vaultFile = buildVaultFile(data);
    const encrypted = await encryptVault(JSON.stringify(vaultFile), passphrase);
    await downloadBlob(encrypted);
  }, [passphrase, data]);

  const closeVault = useCallback(() => {
    setData(emptyData);
    setPassphrase(null);
    setIsLocalMode(false);
    setIsDirty(false);
    setFileHandle(null);
    clearDraftFromIDB().catch(() => {});
  }, []);

  const updateExpenses = useCallback((updater: (prev: Expense[]) => Expense[]) => {
    setData(prev => {
      const updated = { ...prev, expenses: updater(prev.expenses) };
      persistDraft(updated);
      setIsDirty(true);
      return updated;
    });
  }, [persistDraft]);

  const updateInvestments = useCallback((updater: (prev: Investment[]) => Investment[]) => {
    setData(prev => {
      const updated = { ...prev, investments: updater(prev.investments) };
      persistDraft(updated);
      setIsDirty(true);
      return updated;
    });
  }, [persistDraft]);

  const updateLoans = useCallback((updater: (prev: Loan[]) => Loan[]) => {
    setData(prev => {
      const updated = { ...prev, loans: updater(prev.loans) };
      persistDraft(updated);
      setIsDirty(true);
      return updated;
    });
  }, [persistDraft]);

  const updateSips = useCallback((updater: (prev: Sip[]) => Sip[]) => {
    setData(prev => {
      const updated = { ...prev, sips: updater(prev.sips) };
      persistDraft(updated);
      setIsDirty(true);
      return updated;
    });
  }, [persistDraft]);

  return (
    <LocalVaultContext.Provider value={{
      isLocalMode, isDirty, data, passphrase, fileHandle,
      openVaultFromFile, createNewVault, saveVaultToFile, downloadVault,
      closeVault, updateExpenses, updateInvestments, updateLoans, updateSips,
    }}>
      {children}
    </LocalVaultContext.Provider>
  );
};

export const useLocalVault = (): LocalVaultContextType => {
  const ctx = useContext(LocalVaultContext);
  if (!ctx) throw new Error('useLocalVault must be used within LocalVaultProvider');
  return ctx;
};

async function downloadBlob(buffer: ArrayBuffer): Promise<void> {
  const blob = new Blob([buffer], { type: 'application/octet-stream' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `finora-vault-${new Date().toISOString().slice(0, 10)}.enc`;
  document.body.appendChild(a);
  a.click();
  setTimeout(() => {
    URL.revokeObjectURL(url);
    document.body.removeChild(a);
  }, 100);
}
