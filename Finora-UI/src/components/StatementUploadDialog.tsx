import { useState } from 'react';
import { Button } from './ui/Button';
import { Dialog } from './ui/Dialog';
import { Input } from './ui/Input';
import { Select } from './ui/Select';
import { Badge } from './ui/Badge';
import statementApi, {
  StatementPreviewResponse,
  ImportStatus,
  StatementConfirmRequest,
  StatementImportResult,
} from '../api/statementApi';
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
  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [statementType, setStatementType] = useState<StatementType>('CAS');
  const [file, setFile] = useState<File | null>(null);
  const [password, setPassword] = useState('');
  const [preview, setPreview] = useState<StatementPreviewResponse | null>(null);
  const [selectedTab, setSelectedTab] = useState<'equities' | 'mfs'>('equities');
  const [selectedEquities, setSelectedEquities] = useState<Set<string>>(new Set());
  const [selectedMfs, setSelectedMfs] = useState<Set<string>>(new Set());
  const [result, setResult] = useState<StatementImportResult | null>(null);
  const [loading, setLoading] = useState(false);

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
      setStep(2);
      if (data.warnings.length > 0) {
        toast.info(`${data.warnings.length} warnings during parse`);
      }
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Parse failed');
    } finally {
      setLoading(false);
    }
  };

  const toggleEquity = (key: string) => {
    const next = new Set(selectedEquities);
    next.has(key) ? next.delete(key) : next.add(key);
    setSelectedEquities(next);
  };

  const toggleMf = (key: string) => {
    const next = new Set(selectedMfs);
    next.has(key) ? next.delete(key) : next.add(key);
    setSelectedMfs(next);
  };

  const toggleAllEquities = () => {
    if (selectedEquities.size === preview?.holdings.length) {
      setSelectedEquities(new Set());
    } else {
      setSelectedEquities(new Set(preview?.holdings.map(e => e.isin ?? e.symbol) || []));
    }
  };

  const toggleAllMfs = () => {
    if (selectedMfs.size === preview?.mfHoldings.length) {
      setSelectedMfs(new Set());
    } else {
      setSelectedMfs(new Set(preview?.mfHoldings.map(m => m.isin ?? m.schemeName) || []));
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
      setStep(3);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Import failed');
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
    onClose(step === 3);
  };

  return (
    <Dialog isOpen={isOpen} onClose={handleClose} title="Import Statement">
      <div className="max-w-2xl">
        {/* Step 1: Select & Upload */}
        {step === 1 && (
          <div className="space-y-6">
            {/* Statement Type Selection */}
            <div className="space-y-3">
              <label className="text-sm font-semibold">Statement Type</label>
              <div className="grid grid-cols-3 gap-3">
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
                    {type === 'HOLDINGS_FILE' ? 'Holdings File' : type}
                  </button>
                ))}
              </div>
            </div>

            {/* File Drop Zone */}
            <div className="space-y-2">
              <label className="text-sm font-semibold">Upload File</label>
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

            {/* Password Field */}
            {statementType !== 'HOLDINGS_FILE' && (
              <div className="space-y-2">
                <label htmlFor="password" className="text-sm font-semibold">
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

            {/* Actions */}
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

        {/* Step 2: Preview & Selection */}
        {step === 2 && preview && (
          <div className="space-y-4">
            {/* Tabs */}
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

            {/* Equities Table */}
            {selectedTab === 'equities' && (
              <div className="space-y-3">
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b dark:border-gray-700">
                        <th className="text-left p-2 w-8">
                          <input
                            type="checkbox"
                            checked={selectedEquities.size === preview.holdings.length}
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
                        return (
                        <tr key={eKey} className="border-b hover:bg-gray-50 dark:border-gray-700 dark:hover:bg-gray-800">
                          <td className="p-2">
                            <input
                              type="checkbox"
                              checked={selectedEquities.has(eKey)}
                              onChange={() => toggleEquity(eKey)}
                            />
                          </td>
                          <td className="p-2 font-mono text-xs">{e.isin ?? e.symbol}</td>
                          <td className="p-2">{e.name}</td>
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

            {/* MFs Table */}
            {selectedTab === 'mfs' && (
              <div className="space-y-3">
                <div className="overflow-x-auto">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b dark:border-gray-700">
                        <th className="text-left p-2 w-8">
                          <input
                            type="checkbox"
                            checked={selectedMfs.size === preview.mfHoldings.length}
                            onChange={toggleAllMfs}
                          />
                        </th>
                        <th className="text-left p-2">Scheme Code</th>
                        <th className="text-left p-2">Scheme Name</th>
                        <th className="text-right p-2">Units</th>
                        <th className="text-right p-2">Cost/Unit</th>
                        <th className="text-left p-2">Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {preview.mfHoldings.map(m => {
                        const mKey = m.isin ?? m.schemeName;
                        return (
                        <tr key={mKey} className="border-b hover:bg-gray-50 dark:border-gray-700 dark:hover:bg-gray-800">
                          <td className="p-2">
                            <input
                              type="checkbox"
                              checked={selectedMfs.has(mKey)}
                              onChange={() => toggleMf(mKey)}
                            />
                          </td>
                          <td className="p-2 font-mono text-xs">{m.schemeCode ?? m.isin ?? '—'}</td>
                          <td className="p-2">{m.schemeName}</td>
                          <td className="text-right p-2">{m.units.toFixed(2)}</td>
                          <td className="text-right p-2">₹{m.avgCost != null ? m.avgCost.toFixed(2) : '—'}</td>
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

            {/* Warnings */}
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

            {/* Actions */}
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

        {/* Step 3: Result */}
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

            <div className="flex justify-end pt-4">
              <Button onClick={handleClose}>Done</Button>
            </div>
          </div>
        )}
      </div>
    </Dialog>
  );
}
