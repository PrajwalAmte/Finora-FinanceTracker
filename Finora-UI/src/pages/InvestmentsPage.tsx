import React, { useState, useEffect, useMemo } from 'react';
import { Plus, TrendingUp, Download, Search, Upload, RefreshCw, PieChart as PieChartIcon, Trash2, ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Badge } from '../components/ui/Badge';
import { Dialog } from '../components/ui/Dialog';
import { InvestmentForm } from '../components/forms/InvestmentForm';
import { InvestmentActions } from '../components/forms/InvestmentActions';
import { StatementUploadDialog } from '../components/StatementUploadDialog';
import { EmptyState } from '../components/ui/EmptyState';
import { Pagination } from '../components/ui/Pagination';
import { Investment, InvestmentSummary } from '../types/Investment';
import { useInvestmentApi, useIsLocalMode } from '../utils/data-context';
import { formatCurrency, formatPercentage, getStatusColorClass } from '../utils/formatters';
import { PieChart } from '../components/charts/PieChart';
import { generateInvestmentReport } from '../utils/excel-generator';
import { toast } from '../utils/notifications';

const ITEMS_PER_PAGE = 10;

type SortKey = 'name' | 'currentPrice' | 'quantity' | 'value' | 'profitLoss' | 'returnPct';
type SortDir = 'asc' | 'desc';

export const InvestmentsPage: React.FC = () => {
  const investmentApi = useInvestmentApi();
  const isLocalMode = useIsLocalMode();
  const [investments, setInvestments] = useState<Investment[]>([]);
  const [summary, setSummary] = useState<InvestmentSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [isImportDialogOpen, setIsImportDialogOpen] = useState(false);
  const [formKey, setFormKey] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [sortKey, setSortKey] = useState<SortKey>('name');
  const [sortDir, setSortDir] = useState<SortDir>('asc');
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [isBulkLoading, setIsBulkLoading] = useState(false);

  useEffect(() => {
    loadInvestments();
  }, []);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm]);

  const loadInvestments = async () => {
    try {
      setIsLoading(true);
      const [investmentsData, summaryData] = await Promise.all([
        investmentApi.getAll(),
        investmentApi.getSummary()
      ]);
      setInvestments(investmentsData);
      setSummary(summaryData);
    } catch {
      setError('Failed to load investments');
      toast.error('Failed to load investments');
    } finally {
      setIsLoading(false);
    }
  };

  const filteredInvestments = investments.filter(investment =>
    investment.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    investment.symbol.toLowerCase().includes(searchTerm.toLowerCase()) ||
    investment.type.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const getValue = (inv: Investment) => inv.currentValue || (inv.quantity * inv.currentPrice);
  const getPL = (inv: Investment) => inv.profitLoss || 0;
  const getRetPct = (inv: Investment) => inv.returnPercentage || 0;

  const sortedInvestments = useMemo(() => {
    const sorted = [...filteredInvestments];
    sorted.sort((a, b) => {
      let cmp = 0;
      switch (sortKey) {
        case 'name': cmp = a.name.localeCompare(b.name); break;
        case 'currentPrice': cmp = a.currentPrice - b.currentPrice; break;
        case 'quantity': cmp = a.quantity - b.quantity; break;
        case 'value': cmp = getValue(a) - getValue(b); break;
        case 'profitLoss': cmp = getPL(a) - getPL(b); break;
        case 'returnPct': cmp = getRetPct(a) - getRetPct(b); break;
      }
      return sortDir === 'asc' ? cmp : -cmp;
    });
    return sorted;
  }, [filteredInvestments, sortKey, sortDir]);

  const totalPages = Math.ceil(sortedInvestments.length / ITEMS_PER_PAGE);
  const paginatedInvestments = sortedInvestments.slice(
    (currentPage - 1) * ITEMS_PER_PAGE,
    currentPage * ITEMS_PER_PAGE
  );

  const handleSort = (key: SortKey) => {
    if (sortKey === key) setSortDir(prev => prev === 'asc' ? 'desc' : 'asc');
    else { setSortKey(key); setSortDir('asc'); }
  };

  const SortIcon = ({ column }: { column: SortKey }) => {
    if (sortKey !== column) return <ArrowUpDown size={14} className="opacity-40" />;
    return sortDir === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />;
  };

  const pageIds = paginatedInvestments.map(i => i.id!).filter(Boolean);
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
    if (!window.confirm(`Delete ${selectedIds.size} investment(s)?`)) return;
    try {
      setIsBulkLoading(true);
      await investmentApi.bulkDelete(Array.from(selectedIds));
      setInvestments(prev => prev.filter(i => !selectedIds.has(i.id!)));
      toast.success(`Deleted ${selectedIds.size} investment(s)`);
      setSelectedIds(new Set());
    } catch {
      toast.error('Failed to delete investments');
    } finally {
      setIsBulkLoading(false);
    }
  };

  const getInvestmentsByType = () => {
    const groupedData: Record<string, number> = {};
    investments.forEach(investment => {
      const type = investment.type;
      const currentValue = getValue(investment);
      groupedData[type] = (groupedData[type] || 0) + currentValue;
    });
    return Object.entries(groupedData).map(([name, value]) => ({ name, value }));
  };

  const handleAddInvestment = async (data: Omit<Investment, 'id'>) => {
    try {
      setIsSubmitting(true);
      const newInvestment = await investmentApi.create(data);
      setInvestments(prev => [...prev, newInvestment]);
      setIsAddDialogOpen(false);
    } catch {
      toast.error('Failed to add investment');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUpdateInvestment = (updatedInvestment: Investment) => {
    setInvestments(prev => prev.map(investment =>
      investment.id === updatedInvestment.id ? updatedInvestment : investment
    ));
  };

  const handleDeleteInvestment = (id: number) => {
    setInvestments(prev => prev.filter(investment => investment.id !== id));
  };

  const handleRefreshPrices = async () => {
    try {
      setIsRefreshing(true);
      await investmentApi.refreshPrices();
      toast.success('Refresh started — prices will update in ~10 seconds');
      setTimeout(async () => {
        await loadInvestments();
        setIsRefreshing(false);
      }, 10_000);
    } catch {
      toast.error('Failed to start price refresh');
      setIsRefreshing(false);
    }
  };

  const handleExportExcel = () => {
    generateInvestmentReport(filteredInvestments, 'Investment Report', summary?.totalValue, summary?.totalProfitLoss);
  };

  const thClass = 'px-4 py-3 text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider cursor-pointer select-none hover:text-neutral-700 dark:hover:text-neutral-200 transition-colors';

  return (
    <div>
      {error && (
        <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 border border-red-200">{error}</div>
      )}
      <div className="flex flex-col sm:flex-row justify-between gap-4 mb-6">
        <div className="w-full sm:w-72">
          <Input placeholder="Search investments..." icon={<Search size={18} />} value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} fullWidth />
        </div>
        <div className="flex gap-2 items-center">
          {selectedIds.size > 0 && (
            <>
              <Badge variant="primary" size="sm">{selectedIds.size} selected</Badge>
              <Button variant="danger" size="sm" iconLeft={<Trash2 size={16} />} onClick={handleBulkDelete} isLoading={isBulkLoading}>Delete</Button>
            </>
          )}
          {!isLocalMode && <Button variant="outline" iconLeft={<RefreshCw size={18} className={isRefreshing ? 'animate-spin' : ''} />} onClick={handleRefreshPrices} disabled={isRefreshing}>
            {isRefreshing ? 'Refreshing…' : 'Refresh Prices'}
          </Button>}
          <Button variant="outline" iconLeft={<Download size={18} />} onClick={handleExportExcel}>Export</Button>
          {!isLocalMode && <Button variant="outline" iconLeft={<Upload size={18} />} onClick={() => setIsImportDialogOpen(true)}>Import Statement</Button>}
          <Button iconLeft={<Plus size={18} />} onClick={() => { setFormKey(prev => prev + 1); setIsAddDialogOpen(true); }}>Add Investment</Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Card className="h-full" isLoading={isLoading}>
          <div className="text-lg font-medium text-neutral-900 dark:text-white mb-1">Total Value</div>
          <div className="text-3xl font-bold text-primary-600">{formatCurrency(summary?.totalValue || 0)}</div>
        </Card>
        <Card className="h-full" isLoading={isLoading}>
          <div className="text-lg font-medium text-neutral-900 dark:text-white mb-1">Total Profit/Loss</div>
          <div className={`text-3xl font-bold ${getStatusColorClass(summary?.totalProfitLoss || 0)}`}>{formatCurrency(summary?.totalProfitLoss || 0)}</div>
        </Card>
        <Card className="h-full" isLoading={isLoading}>
          <div className="text-lg font-medium text-neutral-900 dark:text-white mb-1">Investment Types</div>
          <div className="text-3xl font-bold text-neutral-800 dark:text-white">{getInvestmentsByType().length || 0}</div>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        <div className="lg:col-span-2">
          <Card title="Investment Portfolio" isLoading={isLoading}>
            {filteredInvestments.length === 0 ? (
              <EmptyState
                icon={<PieChartIcon size={48} />}
                title="No investments found"
                description="Track your stocks, mutual funds, and other investments"
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
                          <span className="inline-flex items-center gap-1">Investment <SortIcon column="name" /></span>
                        </th>
                        <th className={`${thClass} text-right`} onClick={() => handleSort('currentPrice')}>
                          <span className="inline-flex items-center gap-1 justify-end">Price <SortIcon column="currentPrice" /></span>
                        </th>
                        <th className={`${thClass} text-right`} onClick={() => handleSort('quantity')}>
                          <span className="inline-flex items-center gap-1 justify-end">Quantity <SortIcon column="quantity" /></span>
                        </th>
                        <th className={`${thClass} text-right`} onClick={() => handleSort('value')}>
                          <span className="inline-flex items-center gap-1 justify-end">Value <SortIcon column="value" /></span>
                        </th>
                        <th className={`${thClass} text-right`} onClick={() => handleSort('profitLoss')}>
                          <span className="inline-flex items-center gap-1 justify-end">P&L <SortIcon column="profitLoss" /></span>
                        </th>
                        <th className={`${thClass} text-right`} onClick={() => handleSort('returnPct')}>
                          <span className="inline-flex items-center gap-1 justify-end">Return % <SortIcon column="returnPct" /></span>
                        </th>
                        <th className="px-4 py-3 text-center text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-200 dark:divide-neutral-700">
                      {paginatedInvestments.map((investment) => (
                        <tr key={investment.id} className={`hover:bg-neutral-50 dark:hover:bg-neutral-800 transition-colors ${selectedIds.has(investment.id!) ? 'bg-primary-50 dark:bg-primary-900/20' : ''}`}>
                          <td className="px-4 py-4 text-center">
                            <input type="checkbox" checked={selectedIds.has(investment.id!)} onChange={() => toggleSelect(investment.id!)} className="rounded border-neutral-300 dark:border-neutral-600 text-primary-600 focus:ring-primary-500" />
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap">
                            <div className="flex items-start">
                              <div className="flex-shrink-0 h-8 w-8 flex items-center justify-center rounded-full bg-primary-100 dark:bg-primary-900">
                                <TrendingUp size={16} className="text-primary-600 dark:text-primary-400" />
                              </div>
                              <div className="ml-3">
                                <div className="text-sm font-medium text-neutral-900 dark:text-white">{investment.name}</div>
                                <div className="flex items-center gap-2">
                                  <div className="text-xs text-neutral-500 dark:text-neutral-400">{investment.symbol}</div>
                                  <Badge variant="outline" size="sm">{investment.type}</Badge>
                                  {investment.importSource && (
                                    <Badge size="sm" variant={investment.importSource === 'CAS' ? 'default' : investment.importSource === 'CAMS' ? 'primary' : 'outline'}>
                                      {investment.importSource === 'ZERODHA_EXCEL' ? 'Broker Import' : investment.importSource}
                                    </Badge>
                                  )}
                                </div>
                              </div>
                            </div>
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-right">{formatCurrency(investment.currentPrice)}</td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-right">{investment.quantity}</td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-right font-medium">{formatCurrency(getValue(investment))}</td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-right">
                            <span className={getStatusColorClass(getPL(investment))}>{formatCurrency(getPL(investment))}</span>
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-right">
                            <span className={getStatusColorClass(getRetPct(investment))}>{formatPercentage(getRetPct(investment))}</span>
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap text-center">
                            <InvestmentActions investment={investment} onUpdate={handleUpdateInvestment} onDelete={handleDeleteInvestment} />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {totalPages > 1 && (
                  <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={setCurrentPage} totalItems={sortedInvestments.length} itemsPerPage={ITEMS_PER_PAGE} className="mt-4" />
                )}
              </>
            )}
          </Card>
        </div>
        <Card title="Allocation by Type" isLoading={isLoading}>
          <PieChart data={getInvestmentsByType()} />
        </Card>
      </div>

      <Dialog isOpen={isAddDialogOpen} onClose={() => setIsAddDialogOpen(false)} title="Add Investment">
        <InvestmentForm key={`add-investment-form-${formKey}`} onSubmit={handleAddInvestment} onCancel={() => setIsAddDialogOpen(false)} isLoading={isSubmitting} />
      </Dialog>

      {!isLocalMode && <StatementUploadDialog isOpen={isImportDialogOpen} onClose={(success) => { setIsImportDialogOpen(false); if (success) loadInvestments(); }} />}
    </div>
  );
};
