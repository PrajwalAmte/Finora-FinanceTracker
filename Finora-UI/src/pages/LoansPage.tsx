import React, { useState, useEffect, useMemo } from 'react';
import { Plus, CreditCard, Download, Search, Wallet, Trash2, ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Badge } from '../components/ui/Badge';
import { Dialog } from '../components/ui/Dialog';
import { LoanForm } from '../components/forms/LoanForm';
import { LoanActions } from '../components/forms/LoanActions';
import { EmptyState } from '../components/ui/EmptyState';
import { Pagination } from '../components/ui/Pagination';
import { Loan, LoanSummary } from '../types/Loan';
import { useLoanApi } from '../utils/data-context';
import { formatCurrency, formatDate } from '../utils/formatters';
import { PieChart } from '../components/charts/PieChart';
import { BarChart } from '../components/charts/BarChart';
import { generateLoanReport } from '../utils/excel-generator';
import { toast } from '../utils/notifications';

const ITEMS_PER_PAGE = 10;

type SortKey = 'name' | 'principalAmount' | 'interestRate' | 'emiAmount' | 'startDate' | 'remainingMonths' | 'currentBalance';
type SortDir = 'asc' | 'desc';

export const LoansPage: React.FC = () => {
  const loanApi = useLoanApi();
  const [loans, setLoans] = useState<Loan[]>([]);
  const [summary, setSummary] = useState<LoanSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [formKey, setFormKey] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);

  const [sortKey, setSortKey] = useState<SortKey>('name');
  const [sortDir, setSortDir] = useState<SortDir>('asc');
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [isBulkLoading, setIsBulkLoading] = useState(false);

  useEffect(() => {
    const loadLoans = async () => {
      try {
        setIsLoading(true);
        const [loansData, summaryData] = await Promise.all([
          loanApi.getAll(),
          loanApi.getSummary()
        ]);
        setLoans(loansData);
        setSummary(summaryData);
      } catch {
        toast.error('Failed to load loans');
      } finally {
        setIsLoading(false);
      }
    };
    loadLoans();
  }, []);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm]);

  const filteredLoans = loans.filter(loan =>
    loan.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    loan.interestType.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const sortedLoans = useMemo(() => {
    const sorted = [...filteredLoans];
    sorted.sort((a, b) => {
      let cmp = 0;
      switch (sortKey) {
        case 'name': cmp = a.name.localeCompare(b.name); break;
        case 'principalAmount': cmp = a.principalAmount - b.principalAmount; break;
        case 'interestRate': cmp = a.interestRate - b.interestRate; break;
        case 'emiAmount': cmp = (a.emiAmount || 0) - (b.emiAmount || 0); break;
        case 'startDate': cmp = new Date(a.startDate).getTime() - new Date(b.startDate).getTime(); break;
        case 'remainingMonths': cmp = (a.remainingMonths || 0) - (b.remainingMonths || 0); break;
        case 'currentBalance': cmp = a.currentBalance - b.currentBalance; break;
      }
      return sortDir === 'asc' ? cmp : -cmp;
    });
    return sorted;
  }, [filteredLoans, sortKey, sortDir]);

  const totalPages = Math.ceil(sortedLoans.length / ITEMS_PER_PAGE);
  const paginatedLoans = sortedLoans.slice(
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

  const pageIds = paginatedLoans.map(l => l.id!).filter(Boolean);
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
    if (!window.confirm(`Delete ${selectedIds.size} loan(s)?`)) return;
    try {
      setIsBulkLoading(true);
      await loanApi.bulkDelete(Array.from(selectedIds));
      setLoans(prev => prev.filter(l => !selectedIds.has(l.id!)));
      toast.success(`Deleted ${selectedIds.size} loan(s)`);
      setSelectedIds(new Set());
    } catch {
      toast.error('Failed to delete loans');
    } finally {
      setIsBulkLoading(false);
    }
  };

  const getLoanDistributionData = () => {
    return filteredLoans.map(loan => ({ name: loan.name, value: loan.currentBalance }));
  };

  const getInterestTypeData = () => {
    const data: Record<string, number> = {};
    filteredLoans.forEach(loan => {
      data[loan.interestType] = (data[loan.interestType] || 0) + loan.currentBalance;
    });
    return Object.entries(data).map(([name, value]) => ({ name, value }));
  };

  const getPrincipalInterestData = () => {
    return filteredLoans.map(loan => ({
      name: loan.name,
      principal: loan.principalAmount,
      interest: loan.totalInterest || 0,
      value: loan.principalAmount
    }));
  };

  const handleAddLoan = async (data: Omit<Loan, 'id'>) => {
    try {
      setIsSubmitting(true);
      const newLoan = await loanApi.create(data);
      setLoans(prev => [...prev, newLoan]);
      setIsAddDialogOpen(false);
    } catch {
      toast.error('Failed to add loan');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUpdateLoan = (updatedLoan: Loan) => {
    setLoans(prev => prev.map(loan => loan.id === updatedLoan.id ? updatedLoan : loan));
  };

  const handleDeleteLoan = (id: number) => {
    setLoans(prev => prev.filter(loan => loan.id !== id));
  };

  const handleExportExcel = () => {
    generateLoanReport(filteredLoans, 'Loan Report', summary?.totalBalance);
  };

  const thClass = 'px-4 py-3 text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider cursor-pointer select-none hover:text-neutral-700 dark:hover:text-neutral-200 transition-colors';

  return (
    <div>
      <div className="flex flex-col sm:flex-row justify-between gap-4 mb-6">
        <div className="w-full sm:w-72">
          <Input placeholder="Search loans..." icon={<Search size={18} />} value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} fullWidth />
        </div>
        <div className="flex gap-2 items-center">
          {selectedIds.size > 0 && (
            <>
              <Badge variant="primary" size="sm">{selectedIds.size} selected</Badge>
              <Button variant="danger" size="sm" iconLeft={<Trash2 size={16} />} onClick={handleBulkDelete} isLoading={isBulkLoading}>Delete</Button>
            </>
          )}
          <Button variant="outline" iconLeft={<Download size={18} />} onClick={handleExportExcel}>Export</Button>
          <Button iconLeft={<Plus size={18} />} onClick={() => { setFormKey(prev => prev + 1); setIsAddDialogOpen(true); }}>Add Loan</Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Card className="h-full" isLoading={isLoading}>
          <div className="text-lg font-medium text-neutral-900 dark:text-white mb-1">Total Balance</div>
          <div className="text-3xl font-bold text-error-600">{formatCurrency(summary?.totalBalance || 0)}</div>
        </Card>
        <Card className="h-full" isLoading={isLoading}>
          <div className="text-lg font-medium text-neutral-900 dark:text-white mb-1">Total Loans</div>
          <div className="text-3xl font-bold text-neutral-800 dark:text-white">{loans.length}</div>
        </Card>
        <Card className="h-full" isLoading={isLoading}>
          <div className="text-lg font-medium text-neutral-900 dark:text-white mb-1">Avg. Interest Rate</div>
          <div className="text-3xl font-bold text-warning-600">
            {loans.length > 0 ? (loans.reduce((sum, loan) => sum + loan.interestRate, 0) / loans.length).toFixed(2) : 0}%
          </div>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        <div className="lg:col-span-2">
          <Card title="Loan Portfolio" isLoading={isLoading}>
            {filteredLoans.length === 0 ? (
              <EmptyState
                icon={<Wallet size={48} />}
                title="No loans found"
                description="Keep track of your loans and EMIs in one place"
                action={<Button variant="primary" iconLeft={<Plus size={18} />} onClick={() => { setFormKey(prev => prev + 1); setIsAddDialogOpen(true); }}>Add Loan</Button>}
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
                          <span className="inline-flex items-center gap-1">Loan <SortIcon column="name" /></span>
                        </th>
                        <th className={`${thClass} text-right`} onClick={() => handleSort('principalAmount')}>
                          <span className="inline-flex items-center gap-1 justify-end">Principal <SortIcon column="principalAmount" /></span>
                        </th>
                        <th className={`${thClass} text-right`} onClick={() => handleSort('interestRate')}>
                          <span className="inline-flex items-center gap-1 justify-end">Interest Rate <SortIcon column="interestRate" /></span>
                        </th>
                        <th className={`${thClass} text-right`} onClick={() => handleSort('emiAmount')}>
                          <span className="inline-flex items-center gap-1 justify-end">EMI <SortIcon column="emiAmount" /></span>
                        </th>
                        <th className={`${thClass} text-right`} onClick={() => handleSort('startDate')}>
                          <span className="inline-flex items-center gap-1 justify-end">Start Date <SortIcon column="startDate" /></span>
                        </th>
                        <th className={`${thClass} text-right`} onClick={() => handleSort('remainingMonths')}>
                          <span className="inline-flex items-center gap-1 justify-end">Remaining <SortIcon column="remainingMonths" /></span>
                        </th>
                        <th className={`${thClass} text-right`} onClick={() => handleSort('currentBalance')}>
                          <span className="inline-flex items-center gap-1 justify-end">Balance <SortIcon column="currentBalance" /></span>
                        </th>
                        <th className="px-4 py-3 text-center text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-200 dark:divide-neutral-700">
                      {paginatedLoans.map((loan) => (
                        <tr key={loan.id} className={`hover:bg-neutral-50 dark:hover:bg-neutral-800 transition-colors ${selectedIds.has(loan.id!) ? 'bg-primary-50 dark:bg-primary-900/20' : ''}`}>
                          <td className="px-4 py-4 text-center">
                            <input type="checkbox" checked={selectedIds.has(loan.id!)} onChange={() => toggleSelect(loan.id!)} className="rounded border-neutral-300 dark:border-neutral-600 text-primary-600 focus:ring-primary-500" />
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap">
                            <div className="flex items-start">
                              <div className="flex-shrink-0 h-8 w-8 flex items-center justify-center rounded-full bg-error-100 dark:bg-error-900">
                                <CreditCard size={16} className="text-error-600 dark:text-error-400" />
                              </div>
                              <div className="ml-3">
                                <div className="text-sm font-medium text-neutral-900 dark:text-white">{loan.name}</div>
                                <div className="flex items-center">
                                  <Badge variant={loan.interestType === 'SIMPLE' ? 'primary' : 'warning'} size="sm">{loan.interestType}</Badge>
                                  {loan.compoundingFrequency && (
                                    <span className="text-xs text-neutral-500 dark:text-neutral-400 ml-2">{loan.compoundingFrequency}</span>
                                  )}
                                </div>
                              </div>
                            </div>
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-right">{formatCurrency(loan.principalAmount)}</td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-right">{loan.interestRate}%</td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-right">{formatCurrency(loan.emiAmount || 0)}</td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-right">{formatDate(loan.startDate)}</td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-right">{loan.remainingMonths || 0} months</td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-right font-medium">{formatCurrency(loan.currentBalance)}</td>
                          <td className="px-4 py-4 whitespace-nowrap text-center">
                            <LoanActions loan={loan} onUpdate={handleUpdateLoan} onDelete={handleDeleteLoan} />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {totalPages > 1 && (
                  <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={setCurrentPage} totalItems={sortedLoans.length} itemsPerPage={ITEMS_PER_PAGE} className="mt-4" />
                )}
              </>
            )}
          </Card>
        </div>
        <Card title="Loan Distribution" isLoading={isLoading}>
          <div className="h-64"><PieChart data={getLoanDistributionData()} height={250} /></div>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card title="Principal vs Interest" isLoading={isLoading}>
          <BarChart data={getPrincipalInterestData()} bars={[{ dataKey: 'principal', name: 'Principal', color: '#3867D6' }, { dataKey: 'interest', name: 'Interest', color: '#E53E3E' }]} stacked={true} height={300} />
        </Card>
        <Card title="Interest Type Distribution" isLoading={isLoading}>
          <PieChart data={getInterestTypeData()} height={300} />
        </Card>
      </div>

      <Dialog isOpen={isAddDialogOpen} onClose={() => setIsAddDialogOpen(false)} title="Add Loan">
        <LoanForm key={`add-loan-form-${formKey}`} onSubmit={handleAddLoan} onCancel={() => setIsAddDialogOpen(false)} isLoading={isSubmitting} />
      </Dialog>
    </div>
  );
};
