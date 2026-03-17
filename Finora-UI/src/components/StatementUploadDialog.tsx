import { useState } from 'react';
import { Button } from './ui/Button';
import { Dialog } from './ui/Dialog';
import { Input } from './ui/Input';
import { Badge } from './ui/Badge';
import statementApi, {
  StatementPreviewResponse,
  ImportStatus,
  StatementConfirmRequest,
  StatementImportResult,
  ParsedMFHolding,
} from '../api/statementApi';
import { sipApi } from '../api/sipApi';
import axios from 'axios';
import { toast } from '../utils/notifications';

type StatementType = 'CAS' | 'CAMS' | 'HOLDINGS_FILE';

const passwordHints: Record<StatementType, string> = {
  CAS: 'PAN (lowercase) + DOB as DDMMYYYY — e.g., abcde1234f01011990',
  CAMS: 'Email registered with CAMS',
  HOLDINGS_FILE: 'No password required',
};

/** Determines backend statementType from UI type + file extension. */
function getBackendType(type: StatementType, fileName: string): string {
  if (type === 'HOLDINGS_FILE') {
    const ext = fileName.split('.').pop()?.toLowerCase();
    return ext === 'csv' ? 'CSV' : 'EXCEL';
  }
  return type;
}

interface StatementUploadDialogProps {
  isOpen: boolean;
  onClose: (success?: boolean) => void;
}

export function StatementUploadDialog({ isOpen, onClose }: StatementUploadDialogProps) {
  const [step, setStep] = useState<1 | 2 | 3 | 4>(1);
  const [statementType, setStatementType] = useState<StatementType>('CAS');
  const [file, setFile] = useState<File | null>(null);
  const [password, setPassword] = useState('');
  const [preview, setPreview] = useState<StatementPreviewResponse | null>(null);
  const [selectedTab, setSelectedTab] = useState<'equities' | 'mfs'>('equities');
  const [selectedEquities, setSelectedEquities] = useState<Set<string>>(new Set());
  const [selectedMfs, setSelectedMfs] = useState<Set<string>>(new Set());
  const [result, setResult] = useState<StatementImportResult | null>(null);
  const [loading, setLoading] = useState(false);

  const [importedMfs, setImportedMfs] = useState<ParsedMFHolding[]>([]);
  type SipSetup = { enabled: boolean; amount: string; startDate: string };
  const [sipSetups, setSipSetups] = useState<Record<string, SipSetup>>({});

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (f) {
      const ext = f.name.split('.').pop()?.toLowerCase();
      if (statementType === 'HOLDINGS_FILE') {
        if (ext !== 'xlsx' && ext !== 'xls' && ext !== 'csv') {
          toast.error('Expected .xlsx, .xls, or .csv Holdings export file');
          return;
        }
      } else {
        if (ext !== 'pdf') {
          toast.error(`Expected .pdf file for ${statementType}`);
          return;
        }
      }
      setFile(f);
    }
  };

  const handlePreview = async () => {
    if (!file) {
      toast.error('Please select a file');
      return;
    }
    if (statementType !== 'HOLDINGS_FILE' && !password) {
      toast.error('Password required');
      return;
    }

    setLoading(true);
    try {
      const backendType = getBackendType(statementType, file.name);
      const data = await statementApi.preview(file, backendType, password);
      setPreview(data);

      const newEquities = new Set(
        data.holdings
          .filter(h => h.status === ImportStatus.NEW)
          .map(h => h.isin ?? h.symbol)
      );
      const newMfs = new Set(
        data.mfHoldings
          .filter(m => m.status === ImportStatus.NEW)
          .map(m => m.isin ?? m.schemeName)
      );
      setSelectedEquities(newEquities);
      setSelectedMfs(newMfs);

      setStep(2);
      const updateCount =
        data.holdings.filter(h => h.status === ImportStatus.UPDATE).length +
        data.mfHoldings.filter(m => m.status === ImportStatus.UPDATE).length;
      if (updateCount > 0) {
        toast.info(
          `${updateCount} holding${updateCount > 1 ? 's' : ''} already exist — tick them to overwrite.`
        );
      }
      if (data.warnings.length > 0) {
        toast.info(`${data.warnings.length} warnings during parse`);
      }
    } catch (err: unknown) {
      const msg = axios.isAxiosError(err)
        ? (err.response?.data?.message ?? err.message)
        : err instanceof Error ? err.message : 'Parse failed';
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const toggleEquity = (key: string) => {
    const next = new Set(selectedEquities);
    if (next.has(key)) { next.delete(key); } else { next.add(key); }
    setSelectedEquities(next);
  };

  const toggleMf = (key: string) => {
    const next = new Set(selectedMfs);
    if (next.has(key)) { next.delete(key); } else { next.add(key); }
    setSelectedMfs(next);
  };

  const selectableEquities = (preview?.holdings ?? []).filter(
    h => h.status !== ImportStatus.SKIP_MANUAL
  );
  const selectableMfs = (preview?.mfHoldings ?? []).filter(
    m => m.status !== ImportStatus.SKIP_MANUAL
  );

  const toggleAllEquities = () => {
    if (selectedEquities.size === selectableEquities.length) {
      setSelectedEquities(new Set());
    } else {
      setSelectedEquities(new Set(selectableEquities.map(e => e.isin ?? e.symbol)));
    }
  };

  const toggleAllMfs = () => {
    if (selectedMfs.size === selectableMfs.length) {
      setSelectedMfs(new Set());
    } else {
      setSelectedMfs(new Set(selectableMfs.map(m => m.isin ?? m.schemeName)));
    }
  };

  const handleConfirm = async () => {
    if (selectedEquities.size === 0 && selectedMfs.size === 0) {
      toast.error('Select at least one holding');
      return;
    }

    setLoading(true);
    try {
      const req: StatementConfirmRequest = {
        selectedIsins: [...Array.from(selectedEquities), ...Array.from(selectedMfs)],
        statementType: getBackendType(statementType, file?.name ?? ''),
        holdings: preview?.holdings ?? [],
        mfHoldings: preview?.mfHoldings ?? [],
      };
      const res = await statementApi.confirm(req);
      setResult(res);

      if (selectedMfs.size > 0) {
        const mfsForSip = (preview?.mfHoldings ?? []).filter(m => {
          const key = m.isin ?? m.schemeName;
          return selectedMfs.has(key) && m.status !== ImportStatus.SKIP && m.status !== ImportStatus.SKIP_MANUAL;
        });
        if (mfsForSip.length > 0) {
          setImportedMfs(mfsForSip);
          const nextMonth = new Date();
          nextMonth.setMonth(nextMonth.getMonth() + 1);
          const nextMonthStr = nextMonth.toISOString().split('T')[0];
          const initialSetups: Record<string, SipSetup> = {};
          mfsForSip.forEach(m => {
            initialSetups[m.isin ?? m.schemeName] = { enabled: false, amount: '', startDate: nextMonthStr };
          });
          setSipSetups(initialSetups);
          setStep(3);
        } else {
          setStep(3);
        }
      } else {
        setStep(3);
      }
    } catch (err: unknown) {
      const msg = axios.isAxiosError(err)
        ? (err.response?.data?.message ?? err.message)
        : err instanceof Error ? err.message : 'Import failed';
      toast.error(msg);
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setStep(1);
    setFile(null);
    setPassword('');
    setPreview(null);
    setSelectedEquities(new Set());
    setSelectedMfs(new Set());
    setResult(null);
    setImportedMfs([]);
    setSipSetups({});
    onClose(step === 3 || step === 4);
  };

  const handleSetupSips = async () => {
    const toCreate = importedMfs.filter(m => {
      const key = m.isin ?? m.schemeName;
      return sipSetups[key]?.enabled && sipSetups[key]?.amount;
    });

    if (toCreate.length === 0) {
      handleClose();
      return;
    }

    setLoading(true);
    let created = 0;
    for (const m of toCreate) {
      const key = m.isin ?? m.schemeName;
      const setup = sipSetups[key];
      try {
        await sipApi.create({
          name: m.schemeName,
          schemeCode: m.schemeCode ?? '',
          monthlyAmount: parseFloat(setup.amount),
          startDate: setup.startDate,
          durationMonths: 120,
          currentNav: m.nav ?? 0,
          totalUnits: m.units,
          isin: m.isin,
          importSource: getBackendType(statementType, file?.name ?? ''),
        });
        created++;
      } catch {
        toast.error(`Failed to create SIP for ${m.schemeName}`);
      }
    }
    setLoading(false);
    if (created > 0) toast.success(`${created} SIP${created > 1 ? 's' : ''} created`);
    handleClose();
  };

  return (
    <Dialog isOpen={isOpen} onClose={handleClose} title="Import Statement">
      <div className="max-w-2xl">
        {step === 1 && (
          <div className="space-y-6">
            <div className="p-4 bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-700 rounded-lg">
              <p className="text-sm text-blue-800 dark:text-blue-200">
                Import your holdings from CAS, CAMS, or broker exports. 
                We'll automatically match your investments and update values.
              </p>
            </div>

            <div className="space-y-3">
              <label className="text-sm font-semibold text-neutral-700 dark:text-neutral-300">Statement Type</label>
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                {(['CAS', 'CAMS', 'HOLDINGS_FILE'] as const).map(type => (
                  <button
                    key={type}
                    onClick={() => {
                      setStatementType(type);
                      setFile(null);
                      setPassword('');
                    }}
                    className={`p-3 rounded border text-sm font-medium transition ${
                      statementType === type
                        ? 'border-blue-500 bg-blue-50 text-blue-700 dark:bg-blue-900/40 dark:text-blue-300 dark:border-blue-400'
                        : 'border-gray-300 dark:border-gray-600 hover:border-gray-400 dark:hover:border-gray-400 text-gray-700 dark:text-gray-300'
                    }`}
                  >
                    <span className="block font-medium">
                      {type === 'HOLDINGS_FILE' ? 'Holdings File' : type}
                    </span>
                    <span className="block text-xs mt-1 opacity-75">
                      {type === 'CAS' && 'Consolidated Account Statement'}
                      {type === 'CAMS' && 'CAMS Statement PDF'}
                      {type === 'HOLDINGS_FILE' && 'Any Broker Excel / CSV'}
                    </span>
                  </button>
                ))}
              </div>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-semibold text-neutral-700 dark:text-neutral-300">Upload File</label>
              <label className="block p-6 border-2 border-dashed rounded-lg text-center cursor-pointer hover:border-blue-500 transition">
                <input
                  type="file"
                  accept={statementType === 'HOLDINGS_FILE' ? '.xlsx,.xls,.csv' : '.pdf'}
                  onChange={handleFileSelect}
                  className="hidden"
                />
                <div className="text-gray-600 dark:text-gray-300">
                  {file ? (
                    <div>
                      <p className="font-medium text-gray-900 dark:text-white">{file.name}</p>
                      <p className="text-xs mt-1">Click to change</p>
                    </div>
                  ) : (
                    <div>
                      <p className="font-medium">Drop file or click to browse</p>
                      <p className="text-xs mt-1">
                        {statementType === 'HOLDINGS_FILE' ? '.xlsx, .xls, .csv' : '.pdf'} files only
                      </p>
                    </div>
                  )}
                </div>
              </label>
            </div>

            {statementType !== 'HOLDINGS_FILE' && (
              <div className="space-y-2">
                <label htmlFor="password" className="text-sm font-semibold text-neutral-700 dark:text-neutral-300">
                  Password
                </label>
                <Input
                  id="password"
                  type="password"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  placeholder="Enter password"
                />
                <p className="text-xs text-gray-600 dark:text-gray-400">{passwordHints[statementType]}</p>
              </div>
            )}

            <div className="flex justify-end gap-2 pt-4">
              <Button variant="outline" onClick={handleClose}>
                Cancel
              </Button>
              <Button
                onClick={handlePreview}
                disabled={!file || loading}
                isLoading={loading}
              >
                Parse Statement
              </Button>
            </div>
          </div>
        )}

        {step === 2 && preview && (
          <div className="space-y-4">
            <div className="border-b flex gap-4">
              <button
                onClick={() => setSelectedTab('equities')}
                className={`px-4 py-2 border-b-2 text-sm font-medium transition ${
                  selectedTab === 'equities'
                    ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                    : 'border-transparent text-gray-500 hover:text-gray-900 dark:text-gray-400 dark:hover:text-gray-100'
                }`}
              >
                Equities & ETFs ({preview.holdings.length})
              </button>
              <button
                onClick={() => setSelectedTab('mfs')}
                className={`px-4 py-2 border-b-2 text-sm font-medium transition ${
                  selectedTab === 'mfs'
                    ? 'border-blue-500 text-blue-600 dark:text-blue-400'
                    : 'border-transparent text-gray-500 hover:text-gray-900 dark:text-gray-400 dark:hover:text-gray-100'
                }`}
              >
                Mutual Funds ({preview.mfHoldings.length})
              </button>
            </div>

            {selectedTab === 'equities' && (
              <div className="space-y-3">
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b dark:border-gray-700">
                        <th className="text-left p-2 w-8">
                          <input
                            type="checkbox"
                            checked={selectableEquities.length > 0 && selectedEquities.size === selectableEquities.length}
                            onChange={toggleAllEquities}
                          />
                        </th>
                        <th className="text-left p-2">ISIN</th>
                        <th className="text-left p-2">Name</th>
                        <th className="text-right p-2">Qty</th>
                        <th className="text-right p-2">Avg Cost</th>
                        <th className="text-right p-2">LTP</th>
                        <th className="text-left p-2">Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {preview.holdings.map(e => {
                        const eKey = e.isin ?? e.symbol;
                        const isSkipManual = e.status === ImportStatus.SKIP_MANUAL;
                        const isUpdate = e.status === ImportStatus.UPDATE;
                        return (
                        <tr key={eKey} className={`border-b dark:border-gray-700 ${
                          isSkipManual
                            ? 'opacity-50'
                            : isUpdate
                              ? 'bg-amber-50/50 dark:bg-amber-900/10 hover:bg-amber-50 dark:hover:bg-amber-900/20'
                              : 'hover:bg-gray-50 dark:hover:bg-gray-800'
                        }`}>
                          <td className="p-2">
                            <input
                              type="checkbox"
                              checked={selectedEquities.has(eKey)}
                              disabled={isSkipManual}
                              onChange={() => !isSkipManual && toggleEquity(eKey)}
                            />
                          </td>
                          <td className="p-2 font-mono text-xs">{e.isin ?? e.symbol}</td>
                          <td className="p-2">
                            <span>{e.name}</span>
                            {isUpdate && (
                              <span className="block text-xs text-amber-600 dark:text-amber-400">
                                Already imported — tick to overwrite
                              </span>
                            )}
                            {isSkipManual && (
                              <span className="block text-xs text-neutral-400">
                                Manual entry — protected
                              </span>
                            )}
                          </td>
                          <td className="text-right p-2">{e.quantity}</td>
                          <td className="text-right p-2">₹{e.avgCost != null ? e.avgCost.toFixed(2) : '—'}</td>
                          <td className="text-right p-2 text-blue-600 dark:text-blue-400">{e.ltp != null ? `₹${e.ltp.toFixed(2)}` : '—'}</td>
                          <td className="p-2">
                            <Badge
                            size="sm"
                            variant={
                              e.status === ImportStatus.NEW
                                ? 'success'
                                : e.status === ImportStatus.UPDATE
                                  ? 'warning'
                                  : 'outline'
                              }
                            >
                              {e.status}
                            </Badge>
                          </td>
                        </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {selectedTab === 'mfs' && (
              <div className="space-y-3">
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b dark:border-gray-700">
                        <th className="text-left p-2 w-8">
                          <input
                            type="checkbox"
                            checked={selectableMfs.length > 0 && selectedMfs.size === selectableMfs.length}
                            onChange={toggleAllMfs}
                          />
                        </th>
                        <th className="text-left p-2">Scheme Code</th>
                        <th className="text-left p-2">Scheme Name</th>
                        <th className="text-right p-2">Units</th>
                        <th className="text-right p-2">Avg Cost</th>
                        <th className="text-right p-2">NAV</th>
                        <th className="text-left p-2">Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {preview.mfHoldings.map(m => {
                        const mKey = m.isin ?? m.schemeName;
                        const isSkipManual = m.status === ImportStatus.SKIP_MANUAL;
                        const isUpdate = m.status === ImportStatus.UPDATE;
                        return (
                        <tr key={mKey} className={`border-b dark:border-gray-700 ${
                          isSkipManual
                            ? 'opacity-50'
                            : isUpdate
                              ? 'bg-amber-50/50 dark:bg-amber-900/10 hover:bg-amber-50 dark:hover:bg-amber-900/20'
                              : 'hover:bg-gray-50 dark:hover:bg-gray-800'
                        }`}>
                          <td className="p-2">
                            <input
                              type="checkbox"
                              checked={selectedMfs.has(mKey)}
                              disabled={isSkipManual}
                              onChange={() => !isSkipManual && toggleMf(mKey)}
                            />
                          </td>
                          <td className="p-2 font-mono text-xs">{m.schemeCode ?? m.isin ?? '—'}</td>
                          <td className="p-2">
                            <span>{m.schemeName}</span>
                            {isUpdate && (
                              <span className="block text-xs text-amber-600 dark:text-amber-400">
                                Already imported — tick to overwrite
                              </span>
                            )}
                            {isSkipManual && (
                              <span className="block text-xs text-neutral-400">
                                Manual entry — protected
                              </span>
                            )}
                          </td>
                          <td className="text-right p-2">{m.units.toFixed(2)}</td>
                          <td className="text-right p-2">₹{m.avgCost != null ? m.avgCost.toFixed(2) : '—'}</td>
                          <td className="text-right p-2 text-blue-600 dark:text-blue-400">{m.nav != null ? `₹${m.nav.toFixed(2)}` : '—'}</td>
                          <td className="p-2">
                            <Badge
                              size="sm"
                              variant={
                                m.status === ImportStatus.NEW
                                  ? 'success'
                                  : m.status === ImportStatus.UPDATE
                                    ? 'warning'
                                    : 'outline'
                              }
                            >
                              {m.status}
                            </Badge>
                          </td>
                        </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>
            )}

            {preview.warnings.length > 0 && (
              <details className="p-3 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700 rounded">
                <summary className="font-medium text-amber-900 dark:text-amber-300 cursor-pointer">
                  {preview.warnings.length} warnings
                </summary>
                <ul className="mt-2 space-y-1 text-sm text-amber-800 dark:text-amber-400">
                  {preview.warnings.map((w: string, i: number) => (
                    <li key={i}>• {w}</li>
                  ))}
                </ul>
              </details>
            )}

            <div className="flex justify-between gap-2 pt-4">
              <Button variant="outline" onClick={() => setStep(1)}>
                Back
              </Button>
              <Button
                onClick={handleConfirm}
                disabled={selectedEquities.size === 0 && selectedMfs.size === 0}
                isLoading={loading}
              >
                Import Selected
              </Button>
            </div>
          </div>
        )}

        {step === 3 && result && (
          <div className="space-y-4">
            <div className="p-4 bg-green-50 border border-green-200 rounded dark:bg-green-900/20 dark:border-green-700">
              <p className="text-lg font-semibold text-green-900 dark:text-green-300">
                ✓ {result.imported} imported · {result.updated} updated · {result.skipped} skipped
              </p>
            </div>

            {Object.keys(result.skippedReasons).length > 0 && (
              <details className="p-3 bg-gray-50 dark:bg-gray-800 border dark:border-gray-700 rounded">
                <summary className="font-medium cursor-pointer">Skipped Items</summary>
                <ul className="mt-2 space-y-1 text-sm text-gray-700 dark:text-gray-300">
                  {Object.entries(result.skippedReasons).map(([id, reason]: [string, unknown]) => (
                    <li key={id}>
                      <span className="font-mono text-xs">{id}</span>: {String(reason)}
                    </li>
                  ))}
                </ul>
              </details>
            )}

            {result.warnings && result.warnings.length > 0 && (
              <details open className="p-3 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700 rounded">
                <summary className="font-medium text-amber-900 dark:text-amber-300 cursor-pointer">
                  {result.warnings.length} action(s) required
                </summary>
                <ul className="mt-2 space-y-1 text-sm text-amber-800 dark:text-amber-400">
                  {result.warnings.map((w, i) => (
                    <li key={i}>• {w}</li>
                  ))}
                </ul>
              </details>
            )}

            <div className="flex justify-end pt-4">
              {importedMfs.length > 0 ? (
                <div className="flex gap-2">
                  <Button variant="outline" onClick={handleClose}>Skip</Button>
                  <Button onClick={() => setStep(4)}>Set up SIPs →</Button>
                </div>
              ) : (
                <Button onClick={handleClose}>Done</Button>
              )}
            </div>
          </div>
        )}

        {step === 4 && (
          <div className="space-y-4">
            <p className="text-sm text-neutral-600 dark:text-neutral-400">
              For each mutual fund below, toggle it on if you invest in it via a monthly SIP. Enter the amount and the date your SIP deducts each month.
            </p>

            <div className="space-y-3 max-h-80 overflow-y-auto pr-1">
              {importedMfs.map(m => {
                const key = m.isin ?? m.schemeName;
                const setup = sipSetups[key] ?? { enabled: false, amount: '', startDate: '' };
                return (
                  <div
                    key={key}
                    className={`rounded-lg border p-3 transition ${
                      setup.enabled
                        ? 'border-blue-400 dark:border-blue-500 bg-blue-50 dark:bg-blue-900/20'
                        : 'border-neutral-200 dark:border-neutral-700'
                    }`}
                  >
                    <label className="flex items-start gap-3 cursor-pointer">
                      <input
                        type="checkbox"
                        className="mt-1 accent-blue-500"
                        checked={setup.enabled}
                        onChange={e =>
                          setSipSetups(prev => ({
                            ...prev,
                            [key]: { ...prev[key], enabled: e.target.checked },
                          }))
                        }
                      />
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-neutral-900 dark:text-white truncate">{m.schemeName}</p>
                        <p className="text-xs text-neutral-500 dark:text-neutral-400">{m.schemeCode ?? m.isin ?? '—'} · {m.units.toFixed(3)} units</p>
                      </div>
                    </label>

                    {setup.enabled && (
                      <div className="mt-3 grid grid-cols-2 gap-3 pl-7">
                        <Input
                          label="Monthly Amount (₹)"
                          type="number"
                          placeholder="e.g. 5000"
                          value={setup.amount}
                          onChange={e =>
                            setSipSetups(prev => ({
                              ...prev,
                              [key]: { ...prev[key], amount: e.target.value },
                            }))
                          }
                          fullWidth
                        />
                        <Input
                          label="Next Installment Date"
                          type="date"
                          value={setup.startDate}
                          onChange={e =>
                            setSipSetups(prev => ({
                              ...prev,
                              [key]: { ...prev[key], startDate: e.target.value },
                            }))
                          }
                          fullWidth
                        />
                      </div>
                    )}
                  </div>
                );
              })}
            </div>

            <div className="flex justify-between pt-4">
              <Button variant="outline" onClick={handleClose}>Skip</Button>
              <Button onClick={handleSetupSips} isLoading={loading}>
                Confirm SIPs
              </Button>
            </div>
          </div>
        )}
      </div>
    </Dialog>
  );
}
