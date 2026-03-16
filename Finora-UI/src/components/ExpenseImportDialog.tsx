import { useState } from 'react';
import { Button } from './ui/Button';
import { Dialog } from './ui/Dialog';
import { Select } from './ui/Select';
import { Input } from './ui/Input';
import { Badge } from './ui/Badge';
import expenseImportApi, {
  ParsedTransaction,
  ExpensePreviewResponse,
  ExpenseImportResult,
} from '../api/expenseImportApi';
import { toast } from '../utils/notifications';
import { EXPENSE_CATEGORIES, PAYMENT_METHODS } from '../constants';
import { formatCurrency } from '../utils/formatters';

interface ExpenseImportDialogProps {
  isOpen: boolean;
  onClose: (success?: boolean) => void;
}

interface EditableRow {
  original: ParsedTransaction;
  selected: boolean;
  description: string;
  category: string;
  paymentMethod: string;
}

export function ExpenseImportDialog({ isOpen, onClose }: ExpenseImportDialogProps) {
  const [step, setStep] = useState<1 | 2 | 3>(1);
  const [file, setFile] = useState<File | null>(null);
  const [preview, setPreview] = useState<ExpensePreviewResponse | null>(null);
  const [rows, setRows] = useState<EditableRow[]>([]);
  const [result, setResult] = useState<ExpenseImportResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [bulkCategory, setBulkCategory] = useState('');
  const [bulkPayment, setBulkPayment] = useState('');

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const f = e.target.files?.[0];
    if (f) {
      const ext = f.name.split('.').pop()?.toLowerCase();
      if (!['csv', 'xlsx', 'xls'].includes(ext || '')) {
        toast.error('Expected .csv, .xlsx, or .xls file');
        return;
      }
      setFile(f);
    }
  };

  const handlePreview = async () => {
    if (!file) {
      toast.error('Please select a file');
      return;
    }
    setLoading(true);
    try {
      const data = await expenseImportApi.preview(file);
      setPreview(data);

      const debits = data.transactions.filter(t => t.type === 'DEBIT');
      setRows(debits.map(t => ({
        original: t,
        selected: true,
        description: t.narration,
        category: '',
        paymentMethod: 'Net Banking',
      })));

      setStep(2);
      if (data.warnings.length > 0) {
        toast.info(`${data.warnings.length} warnings during parse`);
      }
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Failed to parse statement');
    } finally {
      setLoading(false);
    }
  };

  const updateRow = (idx: number, updates: Partial<EditableRow>) => {
    setRows(prev => prev.map((r, i) => i === idx ? { ...r, ...updates } : r));
  };

  const toggleAll = () => {
    const allSelected = rows.every(r => r.selected);
    setRows(prev => prev.map(r => ({ ...r, selected: !allSelected })));
  };

  const applyBulkCategory = () => {
    if (!bulkCategory) return;
    setRows(prev => prev.map(r => r.selected ? { ...r, category: bulkCategory } : r));
    toast.success(`Category set to "${bulkCategory}" for selected rows`);
  };

  const applyBulkPayment = () => {
    if (!bulkPayment) return;
    setRows(prev => prev.map(r => r.selected ? { ...r, paymentMethod: bulkPayment } : r));
    toast.success(`Payment method set for selected rows`);
  };

  const handleConfirm = async () => {
    const selected = rows.filter(r => r.selected);
    if (selected.length === 0) {
      toast.error('Select at least one transaction');
      return;
    }
    const missing = selected.filter(r => !r.category);
    if (missing.length > 0) {
      toast.error(`${missing.length} selected row(s) have no category assigned`);
      return;
    }
    const emptyDesc = selected.filter(r => !r.description.trim());
    if (emptyDesc.length > 0) {
      toast.error(`${emptyDesc.length} selected row(s) have no description`);
      return;
    }

    setLoading(true);
    try {
      const entries = selected.map(r => ({
        date: r.original.date,
        description: r.description.trim(),
        amount: r.original.amount,
        category: r.category,
        paymentMethod: r.paymentMethod,
      }));
      const res = await expenseImportApi.confirm(entries);
      setResult(res);
      setStep(3);
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Import failed');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    const success = step === 3;
    setStep(1);
    setFile(null);
    setPreview(null);
    setRows([]);
    setResult(null);
    setBulkCategory('');
    setBulkPayment('');
    onClose(success);
  };

  const selectedCount = rows.filter(r => r.selected).length;
  const selectedTotal = rows.filter(r => r.selected)
    .reduce((sum, r) => sum + r.original.amount, 0);

  return (
    <Dialog isOpen={isOpen} onClose={handleClose} title="Import Bank Statement">
      <div className="max-w-4xl">
        {step === 1 && (
          <div className="space-y-6">
            <div className="p-4 bg-blue-50 dark:bg-blue-900/30 border border-blue-200 dark:border-blue-700 rounded-lg">
              <p className="text-sm text-blue-800 dark:text-blue-200">
                Upload your bank statement (CSV or Excel). We'll extract debit transactions
                and let you assign a category and description for each before importing.
              </p>
            </div>

            <div className="space-y-2">
              <label className="text-sm font-semibold text-neutral-700 dark:text-neutral-300">Upload Statement</label>
              <label className="block p-6 border-2 border-dashed rounded-lg text-center cursor-pointer hover:border-blue-500 transition">
                <input
                  type="file"
                  accept=".csv,.xlsx,.xls"
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
                      <p className="text-xs mt-1">.csv, .xlsx, .xls files</p>
                    </div>
                  )}
                </div>
              </label>
            </div>

            <div className="flex justify-end gap-2 pt-4">
              <Button variant="outline" onClick={handleClose}>Cancel</Button>
              <Button onClick={handlePreview} disabled={!file || loading} isLoading={loading}>
                Parse Statement
              </Button>
            </div>
          </div>
        )}

        {step === 2 && preview && (
          <div className="space-y-4">
            <div className="flex flex-wrap items-center gap-3">
              {preview.bankName !== 'Unknown' && (
                <Badge variant="primary" size="sm">{preview.bankName}</Badge>
              )}
              <Badge variant="default" size="sm">{preview.totalDebits} debits</Badge>
              <Badge variant="outline" size="sm">{preview.totalCredits} credits (skipped)</Badge>
              <span className="text-sm text-neutral-500 dark:text-neutral-400 ml-auto">
                {selectedCount} selected · {formatCurrency(selectedTotal)}
              </span>
            </div>

            <div className="flex flex-wrap gap-2 items-end p-3 bg-neutral-50 dark:bg-neutral-800 rounded-lg border border-neutral-200 dark:border-neutral-700">
              <div className="flex-1 min-w-[140px]">
                <Select
                  label="Bulk Category"
                  value={bulkCategory}
                  onChange={e => setBulkCategory(e.target.value)}
                  options={[
                    { value: '', label: 'Select category...' },
                    ...EXPENSE_CATEGORIES.map(c => ({ value: c, label: c }))
                  ]}
                  fullWidth
                />
              </div>
              <Button variant="outline" size="sm" onClick={applyBulkCategory} disabled={!bulkCategory}>
                Apply to Selected
              </Button>
              <div className="flex-1 min-w-[140px]">
                <Select
                  label="Bulk Payment"
                  value={bulkPayment}
                  onChange={e => setBulkPayment(e.target.value)}
                  options={[
                    { value: '', label: 'Select method...' },
                    ...PAYMENT_METHODS.map(m => ({ value: m, label: m }))
                  ]}
                  fullWidth
                />
              </div>
              <Button variant="outline" size="sm" onClick={applyBulkPayment} disabled={!bulkPayment}>
                Apply
              </Button>
            </div>

            <div className="overflow-x-auto max-h-[400px] overflow-y-auto border rounded-lg dark:border-neutral-700">
              <table className="w-full text-sm">
                <thead className="sticky top-0 bg-white dark:bg-neutral-900 z-10">
                  <tr className="border-b dark:border-gray-700">
                    <th className="text-left p-2 w-8">
                      <input type="checkbox" checked={rows.length > 0 && rows.every(r => r.selected)} onChange={toggleAll} />
                    </th>
                    <th className="text-left p-2 w-24">Date</th>
                    <th className="text-left p-2">Description</th>
                    <th className="text-right p-2 w-24">Amount</th>
                    <th className="text-left p-2 w-36">Category</th>
                    <th className="text-left p-2 w-32">Payment</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row, idx) => (
                    <tr
                      key={idx}
                      className={`border-b dark:border-gray-700 ${
                        !row.selected ? 'opacity-40' : row.category ? '' : 'bg-amber-50/50 dark:bg-amber-900/10'
                      }`}
                    >
                      <td className="p-2">
                        <input
                          type="checkbox"
                          checked={row.selected}
                          onChange={() => updateRow(idx, { selected: !row.selected })}
                        />
                      </td>
                      <td className="p-2 text-xs font-mono whitespace-nowrap">{row.original.date}</td>
                      <td className="p-2">
                        <input
                          type="text"
                          value={row.description}
                          onChange={e => updateRow(idx, { description: e.target.value })}
                          className="w-full px-2 py-1 text-sm border rounded bg-transparent dark:border-neutral-600 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                          placeholder="Enter reason / description"
                        />
                        {row.original.narration && row.description !== row.original.narration && (
                          <p className="text-xs text-neutral-400 mt-0.5 truncate max-w-[300px]">{row.original.narration}</p>
                        )}
                      </td>
                      <td className="p-2 text-right font-medium text-red-600 dark:text-red-400 whitespace-nowrap">
                        {formatCurrency(row.original.amount)}
                      </td>
                      <td className="p-2">
                        <select
                          value={row.category}
                          onChange={e => updateRow(idx, { category: e.target.value })}
                          className={`w-full px-1 py-1 text-sm border rounded bg-transparent dark:border-neutral-600 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500 ${
                            row.selected && !row.category ? 'border-amber-400 dark:border-amber-500' : ''
                          }`}
                        >
                          <option value="">Select...</option>
                          {EXPENSE_CATEGORIES.map(c => (
                            <option key={c} value={c}>{c}</option>
                          ))}
                        </select>
                      </td>
                      <td className="p-2">
                        <select
                          value={row.paymentMethod}
                          onChange={e => updateRow(idx, { paymentMethod: e.target.value })}
                          className="w-full px-1 py-1 text-sm border rounded bg-transparent dark:border-neutral-600 dark:text-white focus:outline-none focus:ring-1 focus:ring-blue-500"
                        >
                          {PAYMENT_METHODS.map(m => (
                            <option key={m} value={m}>{m}</option>
                          ))}
                        </select>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {preview.warnings.length > 0 && (
              <details className="p-3 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-700 rounded">
                <summary className="font-medium text-amber-900 dark:text-amber-300 cursor-pointer">
                  {preview.warnings.length} warnings
                </summary>
                <ul className="mt-2 space-y-1 text-sm text-amber-800 dark:text-amber-400">
                  {preview.warnings.map((w, i) => <li key={i}>• {w}</li>)}
                </ul>
              </details>
            )}

            <div className="flex justify-between gap-2 pt-4">
              <Button variant="outline" onClick={() => setStep(1)}>Back</Button>
              <Button onClick={handleConfirm} disabled={selectedCount === 0} isLoading={loading}>
                Import {selectedCount} Expense{selectedCount !== 1 ? 's' : ''}
              </Button>
            </div>
          </div>
        )}

        {step === 3 && result && (
          <div className="space-y-4">
            <div className="p-4 bg-green-50 border border-green-200 rounded dark:bg-green-900/20 dark:border-green-700">
              <p className="text-lg font-semibold text-green-900 dark:text-green-300">
                ✓ {result.imported} expense{result.imported !== 1 ? 's' : ''} imported
                {result.skipped > 0 && ` · ${result.skipped} skipped`}
              </p>
            </div>
            <div className="flex justify-end pt-4">
              <Button onClick={handleClose}>Done</Button>
            </div>
          </div>
        )}
      </div>
    </Dialog>
  );
}
