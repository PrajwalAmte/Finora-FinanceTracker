import React, { useState, useEffect, useMemo } from 'react';
import { Plus, LineChart, Download, Search, Upload, RefreshCw, TrendingUp, Trash2, ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Badge } from '../components/ui/Badge';
import { Dialog } from '../components/ui/Dialog';
import { SipForm } from '../components/forms/SipForm';
import { SipActions } from '../components/forms/SipActions';
import { StatementUploadDialog } from '../components/StatementUploadDialog';
import { EmptyState } from '../components/ui/EmptyState';
import { Pagination } from '../components/ui/Pagination';
import { Sip, SipSummary } from '../types/Sip';
import { sipApi } from '../api/sipApi';
import { formatCurrency, formatDate, getStatusColorClass, formatPercentage } from '../utils/formatters';
import { PieChart } from '../components/charts/PieChart';
import { generateSipReport } from '../utils/excel-generator';
import { toast } from '../utils/notifications';

const ITEMS_PER_PAGE = 10;

type SortKey = 'name' | 'monthlyAmount' | 'currentNav' | 'totalUnits' | 'totalInvested' | 'currentValue' | 'returnPct';
type SortDir = 'asc' | 'desc';

export const SipsPage: React.FC = () => {
  const [sips, setSips] = useState<Sip[]>([]);
  const [summary, setSummary] = useState<SipSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [isImportDialogOpen, setIsImportDialogOpen] = useState(false);
  const [formKey, setFormKey] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [payingId, setPayingId] = useState<number | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);

  const [sortKey, setSortKey] = useState<SortKey>('name');
  const [sortDir, setSortDir] = useState<SortDir>('asc');
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [isBulkLoading, setIsBulkLoading] = useState(false);

  useEffect(() => { loadSips(); }, []);
  useEffect(() => { setCurrentPage(1); }, [searchTerm]);

  const loadSips = async () => {
    try {
      setIsLoading(true);
      const [sipsData, summaryData] = await Promise.all([sipApi.getAll(), sipApi.getSummary()]);
      setSips(sipsData);
      setSummary(summaryData);
    } catch {
      toast.error('Failed to load SIPs');
    } finally {
      setIsLoading(false);
    }
  };

  const filteredSips = sips.filter(sip =>
    sip.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (sip.schemeCode ?? '').toLowerCase().includes(searchTerm.toLowerCase())
  );

  const getRetPct = (s: Sip) => {
    if (!s.totalInvested || s.totalInvested === 0) return 0;
    return ((s.currentValue || 0) - s.totalInvested) / s.totalInvested * 100;
  };

  const sortedSips = useMemo(() => {
    const sorted = [...filteredSips];
    sorted.sort((a, b) => {
      let cmp = 0;
      switch (sortKey) {
        case 'name': cmp = a.name.localeCompare(b.name); break;
        case 'monthlyAmount': cmp = a.monthlyAmount - b.monthlyAmount; break;
        case 'currentNav': cmp = a.currentNav - b.currentNav; break;
        case 'totalUnits': cmp = (a.totalUnits || 0) - (b.totalUnits || 0); break;
        case 'totalInvested': cmp = (a.totalInvested || 0) - (b.totalInvested || 0); break;
        case 'currentValue': cmp = (a.currentValue || 0) - (b.currentValue || 0); break;
        case 'returnPct': cmp = getRetPct(a) - getRetPct(b); break;
      }
      return sortDir === 'asc' ? cmp : -cmp;
    });
    return sorted;
  }, [filteredSips, sortKey, sortDir]);

  const totalPages = Math.ceil(sortedSips.length / ITEMS_PER_PAGE);
  const paginatedSips = sortedSips.slice((currentPage - 1) * ITEMS_PER_PAGE, currentPage * ITEMS_PER_PAGE);

  const handleSort = (key: SortKey) => {
    if (sortKey === key) setSortDir(prev => prev === 'asc' ? 'desc' : 'asc');
    else { setSortKey(key); setSortDir('asc'); }
  };

  const SortIcon = ({ column }: { column: SortKey }) => {
    if (sortKey !== column) return <ArrowUpDown size={14} className="opacity-40" />;
    return sortDir === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />;
  };

  const pageIds = paginatedSips.map(s => s.id!).filter(Boolean);
  const allPageSelected = pageIds.length > 0 && pageIds.every(id => selectedIds.has(id));

  const toggleSelectAll = () => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (allPageSelected) pageIds.forEach(id => next.delete(id));
      else pageIds.forEach(id => next.add(id));
      return next;
    });
  };

  const toggleSelect = (id: number) => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id); else next.add(id);
      return next;
    });
  };

  const handleBulkDelete = async () => {
    if (selectedIds.size === 0) return;
    if (!window.confirm(`Delete ${selectedIds.size} SIP(s)?`)) return;
    try {
      setIsBulkLoading(true);
      await sipApi.bulkDelete(Array.from(selectedIds));
      setSips(prev => prev.filter(s => !selectedIds.has(s.id!)));
      toast.success(`Deleted ${selectedIds.size} SIP(s)`);
      setSelectedIds(new Set());
    } catch {
      toast.error('Failed to delete SIPs');
    } finally {
      setIsBulkLoading(false);
    }
  };

  const calculateReturnPercentage = (invested: number, currentValue: number): number => {
    if (invested === 0) return 0;
    return ((currentValue - invested) / invested) * 100;
  };

  const getSipData = () => filteredSips.map(sip => ({ name: sip.name, value: sip.currentValue || 0 }));

  const handleAddSip = async (data: Omit<Sip, 'id'>) => {
    try {
      setIsSubmitting(true);
      const newSip = await sipApi.create(data);
      setSips(prev => [...prev, newSip]);
      setIsAddDialogOpen(false);
    } catch {
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUpdateSip = (updatedSip: Sip) => {
    setSips(prev => prev.map(sip => sip.id === updatedSip.id ? updatedSip : sip));
  };

  const handleDeleteSip = (id: number) => {
    setSips(prev => prev.filter(sip => sip.id !== id));
  };

  const handleRefreshNavs = async () => {
    try {
      setIsRefreshing(true);
      await sipApi.refreshNavs();
      toast.success('Refresh started — NAVs will update in ~10 seconds');
      setTimeout(async () => {
        await loadSips();
        setIsRefreshing(false);
      }, 10_000);
    } catch {
      toast.error('Failed to start NAV refresh');
      setIsRefreshing(false);
    }
  };

  const handleExportExcel = () => {
    generateSipReport(filteredSips, 'SIP Report', summary?.totalInvestment, summary?.totalCurrentValue, summary?.totalProfitLoss);
  };

  const isPaymentDue = (sip: Sip): boolean => {
    if (!sip.startDate) return false;
    const nextDate = new Date(sip.startDate);
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    nextDate.setHours(0, 0, 0, 0);
    const diffDays = (nextDate.getTime() - today.getTime()) / 86_400_000;
    return diffDays <= 3;
  };

  const handleMarkPaid = async (sip: Sip) => {
    if (!sip.id) return;
    try {
      setPayingId(sip.id);
      const updated = await sipApi.pay(sip.id);
      setSips(prev => prev.map(s => s.id === updated.id ? updated : s));
      const summaryData = await sipApi.getSummary();
      setSummary(summaryData);
      toast.success(`Payment recorded for ${sip.name}`);
    } catch {
      toast.error('Failed to record payment');
    } finally {
      setPayingId(null);
    }
  };

  const thClass = 'px-4 py-3 text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider cursor-pointer select-none hover:text-neutral-700 dark:hover:text-neutral-200 transition-colors';

  return (
    <div>
      <div className="flex flex-col sm:flex-row justify-between gap-4 mb-6">
        <div className="w-full sm:w-72">
          <Input placeholder="Search SIPs..." icon={<Search size={18} />} value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} fullWidth />
        </div>
        <div className="flex gap-2 items-center">
          {selectedIds.size > 0 && (
            <>
              <Badge variant="primary" size="sm">{selectedIds.size} selected</Badge>
              <Button variant="danger" size="sm" iconLeft={<Trash2 size={16} />} onClick={handleBulkDelete} isLoading={isBulkLoading}>Delete</Button>
            </>
          )}
          <Button variant="outline" iconLeft={<RefreshCw size={18} className={isRefreshing ? 'animate-spin' : ''} />} onClick={handleRefreshNavs} disabled={isRefreshing}>
            {isRefreshing ? 'Refreshing…' : 'Refresh NAVs'}
          </Button>
          <Button variant="outline" iconLeft={<Download size={18} />} onClick={handleExportExcel}>Export</Button>
          <Button variant="outline" iconLeft={<Upload size={18} />} onClick={() => setIsImportDialogOpen(true)}>Import Statement</Button>
          <Button iconLeft={<Plus size={18} />} onClick={() => { setFormKey(prev => prev + 1); setIsAddDialogOpen(true); }}>Add SIP</Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Card className="h-full" isLoading={isLoading}>
          <div className="text-lg font-medium text-neutral-900 dark:text-white mb-1">Total Invested</div>
          <div className="text-3xl font-bold text-neutral-800 dark:text-white">{formatCurrency(summary?.totalInvestment || 0)}</div>
        </Card>
        <Card className="h-full" isLoading={isLoading}>
          <div className="text-lg font-medium text-neutral-900 dark:text-white mb-1">Current Value</div>
          <div className="text-3xl font-bold text-primary-600">{formatCurrency(summary?.totalCurrentValue || 0)}</div>
        </Card>
        <Card className="h-full" isLoading={isLoading}>
          <div className="text-lg font-medium text-neutral-900 dark:text-white mb-1">Total Returns</div>
          <div className={`text-3xl font-bold ${getStatusColorClass(summary?.totalProfitLoss || 0)}`}>
            {formatCurrency(summary?.totalProfitLoss || 0)}
            <span className={`text-sm ml-2 ${getStatusColorClass(summary?.totalProfitLoss || 0)}`}>
              ({formatPercentage(calculateReturnPercentage(summary?.totalInvestment || 0, summary?.totalCurrentValue || 0))})
            </span>
          </div>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        <div className="lg:col-span-2">
          <Card title="SIP Plans" isLoading={isLoading}>
            {filteredSips.length === 0 ? (
              <EmptyState
                icon={<TrendingUp size={48} />}
                title="No SIPs found"
                description="SIPs help you invest regularly in mutual funds"
                action={
                  <div className="flex flex-col sm:flex-row gap-2">
                    <Button variant="outline" iconLeft={<Upload size={18} />} onClick={() => setIsImportDialogOpen(true)}>Import Statement</Button>
                    <Button variant="primary" iconLeft={<Plus size={18} />} onClick={() => { setFormKey(prev => prev + 1); setIsAddDialogOpen(true); }}>Add Manually</Button>
                  </div>
                }
              />
            ) : (
              <>
                <div className="overflow-x-auto">
                  <table className="w-full divide-y divide-neutral-200 dark:divide-neutral-700">
                    <thead>
                      <tr>
                        <th className="px-4 py-3 text-center w-10">
                          <input type="checkbox" checked={allPageSelected} onChange={toggleSelectAll} className="rounded border-neutral-300 dark:border-neutral-600 text-primary-600 focus:ring-primary-500" />
                        </th>
                        <th className={`${thClass} text-left`} onClick={() => handleSort('name')}>
                          <span className="inline-flex items-center gap-1">SIP Name <SortIcon column="name" /></span>
                        </th>
                        <th className={`${thClass} text-right`} onClick={() => handleSort('monthlyAmount')}>
                          <span className="inline-flex items-center gap-1 justify-end">Monthly Amount <SortIcon column="monthlyAmount" /></span>
                        </th>
                        <th className={`${thClass} text-right`} onClick={() => handleSort('currentNav')}>
                          <span className="inline-flex items-center gap-1 justify-end">NAV <SortIcon column="currentNav" /></span>
                        </th>
                        <th className={`${thClass} text-right`} onClick={() => handleSort('totalUnits')}>
                          <span className="inline-flex items-center gap-1 justify-end">Units <SortIcon column="totalUnits" /></span>
                        </th>
                        <th className={`${thClass} text-right`} onClick={() => handleSort('totalInvested')}>
                          <span className="inline-flex items-center gap-1 justify-end">Invested <SortIcon column="totalInvested" /></span>
                        </th>
                        <th className={`${thClass} text-right`} onClick={() => handleSort('currentValue')}>
                          <span className="inline-flex items-center gap-1 justify-end">Current Value <SortIcon column="currentValue" /></span>
                        </th>
                        <th className={`${thClass} text-right`} onClick={() => handleSort('returnPct')}>
                          <span className="inline-flex items-center gap-1 justify-end">Return % <SortIcon column="returnPct" /></span>
                        </th>
                        <th className="px-4 py-3 text-center text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-200 dark:divide-neutral-700">
                      {paginatedSips.map((sip) => {
                        const due = isPaymentDue(sip);
                        return (
                          <tr key={sip.id} className={`hover:bg-neutral-50 dark:hover:bg-neutral-800 transition-colors ${due ? 'border-l-4 border-l-amber-400 dark:border-l-amber-500' : ''} ${selectedIds.has(sip.id!) ? 'bg-primary-50 dark:bg-primary-900/20' : ''}`}>
                            <td className="px-4 py-4 text-center">
                              <input type="checkbox" checked={selectedIds.has(sip.id!)} onChange={() => toggleSelect(sip.id!)} className="rounded border-neutral-300 dark:border-neutral-600 text-primary-600 focus:ring-primary-500" />
                            </td>
                            <td className="px-4 py-4 whitespace-nowrap">
                              <div className="flex items-start">
                                <div className="flex-shrink-0 h-8 w-8 flex items-center justify-center rounded-full bg-primary-100 dark:bg-primary-900">
                                  <LineChart size={16} className="text-primary-600 dark:text-primary-400" />
                                </div>
                                <div className="ml-3">
                                  <div className="flex items-center gap-2">
                                    <div className="text-sm font-medium text-neutral-900 dark:text-white">{sip.name}</div>
                                    {sip.investmentId && (
                                      <span className="text-xs px-1.5 py-0.5 rounded bg-blue-100 dark:bg-blue-900/40 text-blue-700 dark:text-blue-300 font-medium">Linked</span>
                                    )}
                                  </div>
                                  <div className="text-xs text-neutral-500 dark:text-neutral-400">Next: {formatDate(sip.startDate)}</div>
                                  {due && (
                                    <div className="flex items-center gap-2 mt-1">
                                      <span className="text-xs font-medium text-amber-600 dark:text-amber-400">Payment due</span>
                                      <button onClick={() => handleMarkPaid(sip)} disabled={payingId === sip.id} className="text-xs px-2 py-0.5 rounded bg-amber-100 dark:bg-amber-900/40 text-amber-800 dark:text-amber-300 hover:bg-amber-200 dark:hover:bg-amber-800/60 disabled:opacity-50 transition">
                                        {payingId === sip.id ? 'Saving…' : 'Mark Paid'}
                                      </button>
                                    </div>
                                  )}
                                </div>
                              </div>
                            </td>
                            <td className="px-4 py-4 whitespace-nowrap text-sm text-right">{formatCurrency(sip.monthlyAmount)}</td>
                            <td className="px-4 py-4 whitespace-nowrap text-sm text-right">{formatCurrency(sip.currentNav)}</td>
                            <td className="px-4 py-4 whitespace-nowrap text-sm text-right">{sip.totalUnits != null ? sip.totalUnits.toFixed(2) : '—'}</td>
                            <td className="px-4 py-4 whitespace-nowrap text-sm text-right">{formatCurrency(sip.totalInvested || 0)}</td>
                            <td className="px-4 py-4 whitespace-nowrap text-sm text-right font-medium">{formatCurrency(sip.currentValue || 0)}</td>
                            <td className="px-4 py-4 whitespace-nowrap text-sm text-right">
                              {sip.totalInvested && sip.currentValue && (
                                <span className={getStatusColorClass(sip.currentValue - sip.totalInvested)}>
                                  {formatPercentage(calculateReturnPercentage(sip.totalInvested, sip.currentValue))}
                                </span>
                              )}
                            </td>
                            <td className="px-4 py-4 whitespace-nowrap text-center">
                              <SipActions sip={sip} onUpdate={handleUpdateSip} onDelete={handleDeleteSip} />
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
                {totalPages > 1 && (
                  <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={setCurrentPage} totalItems={sortedSips.length} itemsPerPage={ITEMS_PER_PAGE} className="mt-4" />
                )}
              </>
            )}
          </Card>
        </div>
        <Card title="SIP Allocation" isLoading={isLoading}>
          <PieChart data={getSipData()} />
        </Card>
      </div>

      <Dialog isOpen={isAddDialogOpen} onClose={() => setIsAddDialogOpen(false)} title="Add SIP">
        <SipForm key={`add-sip-form-${formKey}`} onSubmit={handleAddSip} onCancel={() => setIsAddDialogOpen(false)} isLoading={isSubmitting} />
      </Dialog>

      <StatementUploadDialog isOpen={isImportDialogOpen} onClose={(success) => { setIsImportDialogOpen(false); if (success) loadSips(); }} />
    </div>
  );
};
