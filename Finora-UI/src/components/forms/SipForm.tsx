import React, { useState, useEffect, useRef } from 'react';
import { Input } from '../ui/Input';
import { Button } from '../ui/Button';
import { Sip } from '../../types/Sip';
import { Investment } from '../../types/Investment';
import { useInvestmentApi } from '../../utils/data-context';
import { toast } from '../../utils/notifications';
import { Link2, Link2Off } from 'lucide-react';

interface SipFormData {
  name: string;
  schemeCode: string;
  monthlyAmount: string;
  startDate: string;
  currentNav: string;
  totalUnits: string;
  totalInvested: string;
  investmentId: number | undefined;
}

interface SipFormProps {
  onSubmit: (data: SipFormData) => void;
  onCancel: () => void;
  isLoading?: boolean;
  initialData?: Sip;
  mode?: 'create' | 'edit';
}

export const SipForm: React.FC<SipFormProps> = ({
  onSubmit,
  onCancel,
  isLoading = false,
  initialData,
  mode = 'create',
}) => {
  const investmentApi = useInvestmentApi();
  const defaultNextInstallment = () => {
    const d = new Date();
    d.setMonth(d.getMonth() + 1);
    return d.toISOString().split('T')[0];
  };

  const [formData, setFormData] = useState({
    name: '',
    schemeCode: '',
    monthlyAmount: '',
    startDate: defaultNextInstallment(),
    currentNav: '',
    totalUnits: '',
    totalInvested: '',
    investmentId: undefined as number | undefined,
  });

  // AMFI fund search state
  const [fundQuery, setFundQuery] = useState('');
  const [fundResults, setFundResults] = useState<{ schemeCode: string; name: string; nav: number }[]>([]);
  const [showFundDropdown, setShowFundDropdown] = useState(false);
  const [fundSearching, setFundSearching] = useState(false);
  const searchTimeout = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Existing MF investments for auto-linking
  const [mfInvestments, setMfInvestments] = useState<Investment[]>([]);

  // Load user's MF investments once on mount
  useEffect(() => {
    investmentApi.getAll()
      .then(all => setMfInvestments(all.filter(i => i.type === 'MUTUAL_FUND')))
      .catch(() => {});
  }, []);

  // Populate form when editing
  useEffect(() => {
    if (initialData) {
      setFormData({
        name: initialData.name,
        schemeCode: initialData.schemeCode ?? '',
        monthlyAmount: initialData.monthlyAmount.toString(),
        startDate: initialData.startDate
          ? new Date(initialData.startDate).toISOString().split('T')[0]
          : defaultNextInstallment(),
        currentNav: initialData.currentNav?.toString() ?? '',
        totalUnits: initialData.totalUnits?.toString() ?? '',
        totalInvested: initialData.totalInvested?.toString() ?? '',
        investmentId: initialData.investmentId,
      });
      setFundQuery(initialData.name);
    } else {
      setFormData({
        name: '',
        schemeCode: '',
        monthlyAmount: '',
        startDate: defaultNextInstallment(),
        currentNav: '',
        totalUnits: '',
        totalInvested: '',
        investmentId: undefined,
      });
      setFundQuery('');
    }
  }, [initialData]);

  // Debounced AMFI fund search — only when schemeCode not yet chosen
  useEffect(() => {
    if (formData.schemeCode) return; // fund already selected
    if (fundQuery.trim().length < 2) { setFundResults([]); return; }
    if (searchTimeout.current) clearTimeout(searchTimeout.current);
    searchTimeout.current = setTimeout(async () => {
      setFundSearching(true);
      const results = await investmentApi.searchMf(fundQuery);
      setFundResults(results);
      setShowFundDropdown(results.length > 0);
      setFundSearching(false);
    }, 350);
    return () => { if (searchTimeout.current) clearTimeout(searchTimeout.current); };
  }, [fundQuery, formData.schemeCode]);

  // Auto-link: when schemeCode is resolved, look for a matching MF investment
  useEffect(() => {
    if (!formData.schemeCode || formData.investmentId) return;
    const match = mfInvestments.find(i => i.symbol === formData.schemeCode);
    if (match) {
      setFormData(prev => ({ ...prev, investmentId: match.id }));
      toast.success(`Auto-linked to investment: ${match.name}`);
    }
  }, [formData.schemeCode, mfInvestments]);

  const isLinked = !!formData.investmentId;
  const linkedInvestment = mfInvestments.find(i => i.id === formData.investmentId);

  const handleSelectFund = (f: { schemeCode: string; name: string; nav: number }) => {
    setFormData(prev => ({
      ...prev,
      name: f.name,
      schemeCode: f.schemeCode,
      currentNav: String(f.nav),
      investmentId: undefined, // reset; auto-link effect will re-evaluate
    }));
    setFundQuery(f.name);
    setShowFundDropdown(false);
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const monthlyAmount = parseFloat(formData.monthlyAmount);
    if (!monthlyAmount || monthlyAmount <= 0) {
      toast.error('Monthly amount must be a positive number');
      return;
    }
    if (!formData.schemeCode) {
      toast.error('Please search and select a fund');
      return;
    }
    onSubmit({
      ...formData,
      monthlyAmount,
      durationMonths: 120,
      currentNav: formData.currentNav ? parseFloat(formData.currentNav) : undefined,
      totalUnits: formData.totalUnits ? parseFloat(formData.totalUnits) : undefined,
      totalInvested: formData.totalInvested ? parseFloat(formData.totalInvested) : undefined,
      investmentId: formData.investmentId ?? null,
    });
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">

      {/* ── Fund name search ───────────────────────────────────────────── */}
      <div className="relative">
        <Input
          label="Search Fund Name"
          value={fundQuery}
          onChange={(e) => {
            setFundQuery(e.target.value);
            // Clear selection if user types again
            if (formData.schemeCode) {
              setFormData(prev => ({ ...prev, schemeCode: '', name: '', investmentId: undefined }));
            }
          }}
          onFocus={() => fundResults.length > 0 && setShowFundDropdown(true)}
          onBlur={() => setTimeout(() => setShowFundDropdown(false), 150)}
          placeholder="Type fund name to search AMFI…"
          fullWidth
        />
        {fundSearching && <p className="text-xs text-neutral-400 mt-1">Searching…</p>}
        {formData.schemeCode && (
          <p className="text-xs text-neutral-500 dark:text-neutral-400 mt-1">
            Scheme code:{' '}
            <span className="font-mono text-blue-600 dark:text-blue-400">{formData.schemeCode}</span>
          </p>
        )}
        {showFundDropdown && (
          <ul className="absolute z-50 w-full mt-1 bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 rounded-md shadow-lg max-h-56 overflow-y-auto text-sm">
            {fundResults.map(f => (
              <li
                key={f.schemeCode}
                className="px-3 py-2 cursor-pointer hover:bg-neutral-100 dark:hover:bg-neutral-700"
                onMouseDown={() => handleSelectFund(f)}
              >
                <span className="block font-medium text-neutral-800 dark:text-neutral-100 leading-tight">{f.name}</span>
                <span className="text-neutral-400 text-xs">Code: {f.schemeCode} · NAV: ₹{Number(f.nav).toFixed(2)}</span>
              </li>
            ))}
          </ul>
        )}
      </div>

      {/* ── Investment link status ─────────────────────────────────────── */}
      {isLinked && linkedInvestment ? (
        <div className="flex items-center justify-between rounded-md bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-700 px-3 py-2 text-sm">
          <span className="flex items-center gap-1.5 text-blue-700 dark:text-blue-300">
            <Link2 size={14} />
            Linked to investment: <span className="font-medium">{linkedInvestment.name}</span>
          </span>
          <button
            type="button"
            className="ml-2 text-xs text-red-500 hover:text-red-700 dark:hover:text-red-400 flex items-center gap-1"
            onClick={() => setFormData(prev => ({ ...prev, investmentId: undefined }))}
          >
            <Link2Off size={12} /> Unlink
          </button>
        </div>
      ) : formData.schemeCode && mfInvestments.length > 0 ? (
        /* Manual link picker — shown when auto-link found no match */
        <div>
          <label className="block text-sm font-medium text-neutral-700 dark:text-neutral-300 mb-1">
            Link to existing investment <span className="text-neutral-400 font-normal">(optional)</span>
          </label>
          <select
            className="w-full px-3 py-2 text-sm bg-white dark:bg-neutral-900 border border-neutral-300 dark:border-neutral-700 rounded-md text-neutral-800 dark:text-neutral-100 focus:outline-none focus:ring-2 focus:ring-primary-500"
            value={formData.investmentId ?? ''}
            onChange={e => setFormData(prev => ({
              ...prev,
              investmentId: e.target.value ? Number(e.target.value) : undefined,
            }))}
          >
            <option value="">— none (standalone SIP) —</option>
            {mfInvestments.map(i => (
              <option key={i.id} value={i.id}>{i.name}</option>
            ))}
          </select>
        </div>
      ) : null}

      {/* ── Monthly amount & installment date ─────────────────────────── */}
      <Input
        type="number"
        label="Monthly Amount (₹)"
        value={formData.monthlyAmount}
        onChange={(e) => setFormData({ ...formData, monthlyAmount: e.target.value })}
        required
        fullWidth
        step="any"
        min="0.01"
      />

      <Input
        type="date"
        label="Next Installment Date"
        value={formData.startDate}
        onChange={(e) => setFormData({ ...formData, startDate: e.target.value })}
        required
        fullWidth
      />

      {/* ── Standalone-only fields (hidden when linked to an Investment) ─ */}
      {!isLinked && (
        <>
          <Input
            type="number"
            label="Total Invested So Far (₹) — optional"
            placeholder="Leave blank to start fresh"
            value={formData.totalInvested}
            onChange={(e) => setFormData({ ...formData, totalInvested: e.target.value })}
            fullWidth
            step="any"
          />
          <Input
            type="number"
            label="Current NAV — optional"
            placeholder="Auto-fetched from AMFI"
            value={formData.currentNav}
            onChange={(e) => setFormData({ ...formData, currentNav: e.target.value })}
            fullWidth
            step="any"
          />
          <Input
            type="number"
            label="Total Units — optional"
            placeholder="Auto-fetched from AMFI"
            value={formData.totalUnits}
            onChange={(e) => setFormData({ ...formData, totalUnits: e.target.value })}
            fullWidth
            step="any"
          />
        </>
      )}

      {isLinked && (
        <p className="text-xs text-blue-600 dark:text-blue-400">
          Units, NAV and current value are tracked by the linked investment — no need to enter them here.
        </p>
      )}

      <div className="flex justify-end space-x-2 pt-4">
        <Button type="button" variant="outline" onClick={onCancel} disabled={isLoading}>
          Cancel
        </Button>
        <Button type="submit" isLoading={isLoading}>
          {mode === 'create' ? 'Add SIP' : 'Update SIP'}
        </Button>
      </div>
    </form>
  );
};
