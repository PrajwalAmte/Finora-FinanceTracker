import React, { useState, useEffect, useMemo } from 'react';
import { Plus, Download, Upload, Calendar, Search, Receipt, Trash2, Edit3, ArrowUpDown, ArrowUp, ArrowDown } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';
import { Badge } from '../components/ui/Badge';
import { Dialog } from '../components/ui/Dialog';
import { ExpenseForm } from '../components/forms/ExpenseForm';
import { ExpenseActions } from '../components/forms/ExpenseActions';
import { ExpenseImportDialog } from '../components/ExpenseImportDialog';
import { EmptyState } from '../components/ui/EmptyState';
import { Pagination } from '../components/ui/Pagination';
import { Expense, ExpenseSummary } from '../types/Expense';
import { expenseApi } from '../api/expenseApi';
import { formatCurrency, formatDate } from '../utils/formatters';
import { PieChart } from '../components/charts/PieChart';
import { BarChart } from '../components/charts/BarChart';
import { generateExpenseReport } from '../utils/excel-generator';
import { PAYMENT_METHODS, EXPENSE_CATEGORIES } from '../constants';
import { toast } from '../utils/notifications';

const ITEMS_PER_PAGE = 10;

type SortKey = 'description' | 'date' | 'category' | 'paymentMethod' | 'amount';
type SortDir = 'asc' | 'desc';

export const ExpensesPage: React.FC = () => {
  const [expenses, setExpenses] = useState<Expense[]>([]);
  const [summary, setSummary] = useState<ExpenseSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [paymentMethodFilter, setPaymentMethodFilter] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [startDate, setStartDate] = useState<string>(() => {
    const date = new Date();
    date.setDate(1);
    return date.toISOString().split('T')[0];
  });
  const [endDate, setEndDate] = useState<string>(() => {
    const date = new Date();
    return date.toISOString().split('T')[0];
  });
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [isImportDialogOpen, setIsImportDialogOpen] = useState(false);
  const [formKey, setFormKey] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [sortKey, setSortKey] = useState<SortKey>('date');
  const [sortDir, setSortDir] = useState<SortDir>('desc');
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set());
  const [isBulkEditOpen, setIsBulkEditOpen] = useState(false);
  const [bulkCategory, setBulkCategory] = useState('');
  const [bulkPayment, setBulkPayment] = useState('');
  const [isBulkLoading, setIsBulkLoading] = useState(false);

  useEffect(() => {
    const loadExpenses = async () => {
      try {
        setIsLoading(true);
        const [expensesData, summaryData] = await Promise.all([
          expenseApi.getByDateRange(startDate, endDate),
          expenseApi.getSummary(startDate, endDate)
        ]);
        setExpenses(expensesData);
        setSummary(summaryData);
      } catch {
        setError('Failed to load expenses');
        toast.error('Failed to load expenses');
      } finally {
        setIsLoading(false);
      }
    };
    if (startDate && endDate) loadExpenses();
  }, [startDate, endDate]);

  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm, categoryFilter, paymentMethodFilter]);

  const filteredExpenses = expenses.filter(expense => {
    const matchesSearch = expense.description.toLowerCase().includes(searchTerm.toLowerCase()) ||
                          expense.category.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCategory = !categoryFilter || expense.category === categoryFilter;
    const matchesPaymentMethod = !paymentMethodFilter || expense.paymentMethod === paymentMethodFilter;
    return matchesSearch && matchesCategory && matchesPaymentMethod;
  });

  const sortedExpenses = useMemo(() => {
    const sorted = [...filteredExpenses];
    sorted.sort((a, b) => {
      let cmp = 0;
      switch (sortKey) {
        case 'description': cmp = a.description.localeCompare(b.description); break;
        case 'date': cmp = new Date(a.date).getTime() - new Date(b.date).getTime(); break;
        case 'category': cmp = a.category.localeCompare(b.category); break;
        case 'paymentMethod': cmp = a.paymentMethod.localeCompare(b.paymentMethod); break;
        case 'amount': cmp = a.amount - b.amount; break;
      }
      return sortDir === 'asc' ? cmp : -cmp;
    });
    return sorted;
  }, [filteredExpenses, sortKey, sortDir]);

  const totalPages = Math.ceil(sortedExpenses.length / ITEMS_PER_PAGE);
  const paginatedExpenses = sortedExpenses.slice(
    (currentPage - 1) * ITEMS_PER_PAGE,
    currentPage * ITEMS_PER_PAGE
  );

  const handleSort = (key: SortKey) => {
    if (sortKey === key) {
      setSortDir(prev => prev === 'asc' ? 'desc' : 'asc');
    } else {
      setSortKey(key);
      setSortDir('asc');
    }
  };

  const SortIcon = ({ column }: { column: SortKey }) => {
    if (sortKey !== column) return <ArrowUpDown size={14} className="opacity-40" />;
    return sortDir === 'asc' ? <ArrowUp size={14} /> : <ArrowDown size={14} />;
  };

  const pageIds = paginatedExpenses.map(e => e.id!).filter(Boolean);
  const allPageSelected = pageIds.length > 0 && pageIds.every(id => selectedIds.has(id));

  const toggleSelectAll = () => {
    setSelectedIds(prev => {
      const next = new Set(prev);
      if (allPageSelected) {
        pageIds.forEach(id => next.delete(id));
      } else {
        pageIds.forEach(id => next.add(id));
      }
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
    if (!window.confirm(`Delete ${selectedIds.size} expense(s)?`)) return;
    try {
      setIsBulkLoading(true);
      await expenseApi.bulkDelete(Array.from(selectedIds));
      setExpenses(prev => prev.filter(e => !selectedIds.has(e.id!)));
      toast.success(`Deleted ${selectedIds.size} expense(s)`);
      setSelectedIds(new Set());
    } catch {
      toast.error('Failed to delete expenses');
    } finally {
      setIsBulkLoading(false);
    }
  };

  const handleBulkEdit = async () => {
    if (!bulkCategory && !bulkPayment) {
      toast.error('Select at least one field to update');
      return;
    }
    try {
      setIsBulkLoading(true);
      const fields: { category?: string; paymentMethod?: string } = {};
      if (bulkCategory) fields.category = bulkCategory;
      if (bulkPayment) fields.paymentMethod = bulkPayment;
      await expenseApi.bulkUpdate(Array.from(selectedIds), fields);
      setExpenses(prev => prev.map(e => {
        if (!selectedIds.has(e.id!)) return e;
        return { ...e, ...(bulkCategory && { category: bulkCategory }), ...(bulkPayment && { paymentMethod: bulkPayment }) };
      }));
      toast.success(`Updated ${selectedIds.size} expense(s)`);
      setSelectedIds(new Set());
      setIsBulkEditOpen(false);
      setBulkCategory('');
      setBulkPayment('');
    } catch {
      toast.error('Failed to update expenses');
    } finally {
      setIsBulkLoading(false);
    }
  };

  const getCategoryData = () => {
    const categories: Record<string, number> = {};
    filteredExpenses.forEach(expense => {
      categories[expense.category] = (categories[expense.category] || 0) + expense.amount;
    });
    return Object.entries(categories).map(([name, value]) => ({ name, value }));
  };

  const getPaymentMethodData = () => {
    const methods: Record<string, number> = {};
    filteredExpenses.forEach(expense => {
      methods[expense.paymentMethod] = (methods[expense.paymentMethod] || 0) + expense.amount;
    });
    return Object.entries(methods).map(([name, value]) => ({ name, value }));
  };

  const getMonthlySpendingData = () => {
    const months: Record<string, number> = {};
    const today = new Date();
    for (let i = 5; i >= 0; i--) {
      const date = new Date(today.getFullYear(), today.getMonth() - i, 1);
      const monthKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
      months[monthKey] = 0;
    }
    filteredExpenses.forEach(expense => {
      const date = new Date(expense.date);
      const monthKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
      if (months[monthKey] !== undefined) months[monthKey] += expense.amount;
    });
    return Object.entries(months).map(([key, value]) => {
      const [year, month] = key.split('-');
      const date = new Date(parseInt(year), parseInt(month) - 1, 1);
      return { name: date.toLocaleDateString('en-US', { month: 'short', year: '2-digit' }), value };
    });
  };

  const handleExportExcel = () => {
    generateExpenseReport(filteredExpenses, 'Expense Report', startDate, endDate, summary?.totalExpenses);
  };

  const handleAddExpense = async (data: Omit<Expense, 'id'>) => {
    try {
      setIsSubmitting(true);
      const newExpense = await expenseApi.create(data);
      setExpenses(prev => [...prev, newExpense]);
      setIsAddDialogOpen(false);
    } catch {
      toast.error('Failed to add expense');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUpdateExpense = (updatedExpense: Expense) => {
    setExpenses(prev => prev.map(expense => expense.id === updatedExpense.id ? updatedExpense : expense));
  };

  const handleDeleteExpense = (id: number) => {
    setExpenses(prev => prev.filter(expense => expense.id !== id));
  };

  const handleImportClose = async (success?: boolean) => {
    setIsImportDialogOpen(false);
    if (success) {
      try {
        const [expensesData, summaryData] = await Promise.all([
          expenseApi.getByDateRange(startDate, endDate),
          expenseApi.getSummary(startDate, endDate)
        ]);
        setExpenses(expensesData);
        setSummary(summaryData);
      } catch {
        toast.error('Failed to refresh expenses');
      }
    }
  };

  const thClass = 'px-4 py-3 text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider cursor-pointer select-none hover:text-neutral-700 dark:hover:text-neutral-200 transition-colors';

  return (
    <div>
      {error && (
        <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 border border-red-200">{error}</div>
      )}
      <div className="grid grid-cols-1 sm:grid-cols-12 gap-4 mb-6">
        <div className="sm:col-span-3">
          <Input type="date" label="Start Date" value={startDate} onChange={(e) => setStartDate(e.target.value)} icon={<Calendar size={18} />} fullWidth />
        </div>
        <div className="sm:col-span-3">
          <Input type="date" label="End Date" value={endDate} onChange={(e) => setEndDate(e.target.value)} icon={<Calendar size={18} />} fullWidth />
        </div>
        <div className="sm:col-span-3">
          <Select label="Category" value={categoryFilter} onChange={(e) => setCategoryFilter(e.target.value)} options={[{ value: '', label: 'All Categories' }, ...EXPENSE_CATEGORIES.map(cat => ({ value: cat, label: cat }))]} fullWidth />
        </div>
        <div className="sm:col-span-3 flex items-end gap-2">
          <Button variant="outline" iconLeft={<Download size={18} />} onClick={handleExportExcel} fullWidth>Export</Button>
          <Button variant="outline" iconLeft={<Upload size={18} />} fullWidth onClick={() => setIsImportDialogOpen(true)}>Import</Button>
          <Button iconLeft={<Plus size={18} />} fullWidth onClick={() => { setFormKey(prev => prev + 1); setIsAddDialogOpen(true); }}>Add</Button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Card className="h-full" isLoading={isLoading}>
          <div className="text-lg font-medium text-neutral-900 dark:text-white mb-1">Total Expenses</div>
          <div className="text-3xl font-bold text-error-600">{formatCurrency(summary?.totalExpenses || 0)}</div>
        </Card>
        <Card className="h-full" isLoading={isLoading}>
          <div className="text-lg font-medium text-neutral-900 dark:text-white mb-1">Categories</div>
          <div className="text-3xl font-bold text-neutral-800 dark:text-white">{summary?.expensesByCategory ? Object.keys(summary.expensesByCategory).length : 0}</div>
        </Card>
        <Card className="h-full" isLoading={isLoading}>
          <div className="text-lg font-medium text-neutral-900 dark:text-white mb-1">Transactions</div>
          <div className="text-3xl font-bold text-neutral-800 dark:text-white">{filteredExpenses.length}</div>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        <div className="lg:col-span-2">
          <Card title="Expense Transactions" isLoading={isLoading}>
            <div className="mb-4 flex flex-col sm:flex-row gap-3">
              <div className="flex-1">
                <Input placeholder="Search expenses..." icon={<Search size={18} />} value={searchTerm} onChange={(e) => setSearchTerm(e.target.value)} fullWidth />
              </div>
              {selectedIds.size > 0 && (
                <div className="flex items-center gap-2">
                  <Badge variant="primary" size="sm">{selectedIds.size} selected</Badge>
                  <Button variant="outline" size="sm" iconLeft={<Edit3 size={16} />} onClick={() => setIsBulkEditOpen(true)}>Edit</Button>
                  <Button variant="danger" size="sm" iconLeft={<Trash2 size={16} />} onClick={handleBulkDelete} isLoading={isBulkLoading}>Delete</Button>
                </div>
              )}
            </div>

            {filteredExpenses.length === 0 ? (
              <EmptyState
                icon={<Receipt size={48} />}
                title="No expenses found"
                description="Start tracking your spending by adding your first expense"
                action={<Button variant="primary" iconLeft={<Plus size={18} />} onClick={() => { setFormKey(prev => prev + 1); setIsAddDialogOpen(true); }}>Add Expense</Button>}
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
                        <th className={`${thClass} text-left`} onClick={() => handleSort('description')}>
                          <span className="inline-flex items-center gap-1">Description <SortIcon column="description" /></span>
                        </th>
                        <th className={`${thClass} text-center`} onClick={() => handleSort('date')}>
                          <span className="inline-flex items-center gap-1">Date <SortIcon column="date" /></span>
                        </th>
                        <th className={`${thClass} text-center`} onClick={() => handleSort('category')}>
                          <span className="inline-flex items-center gap-1">Category <SortIcon column="category" /></span>
                        </th>
                        <th className={`${thClass} text-center`} onClick={() => handleSort('paymentMethod')}>
                          <span className="inline-flex items-center gap-1">Payment <SortIcon column="paymentMethod" /></span>
                        </th>
                        <th className={`${thClass} text-right`} onClick={() => handleSort('amount')}>
                          <span className="inline-flex items-center gap-1 justify-end">Amount <SortIcon column="amount" /></span>
                        </th>
                        <th className="px-4 py-3 text-center text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-200 dark:divide-neutral-700">
                      {paginatedExpenses.map((expense) => (
                        <tr key={expense.id} className={`hover:bg-neutral-50 dark:hover:bg-neutral-800 transition-colors ${selectedIds.has(expense.id!) ? 'bg-primary-50 dark:bg-primary-900/20' : ''}`}>
                          <td className="px-4 py-4 text-center">
                            <input type="checkbox" checked={selectedIds.has(expense.id!)} onChange={() => toggleSelect(expense.id!)} className="rounded border-neutral-300 dark:border-neutral-600 text-primary-600 focus:ring-primary-500" />
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap">
                            <div className="text-sm font-medium text-neutral-900 dark:text-white">{expense.description}</div>
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-center text-neutral-500 dark:text-neutral-400">{formatDate(expense.date, 'MMM dd, yyyy')}</td>
                          <td className="px-4 py-4 whitespace-nowrap text-center"><Badge variant="primary" size="sm">{expense.category}</Badge></td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-center text-neutral-500 dark:text-neutral-400">{expense.paymentMethod}</td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-right font-medium text-error-600 dark:text-error-400">{formatCurrency(expense.amount)}</td>
                          <td className="px-4 py-4 whitespace-nowrap text-center">
                            <ExpenseActions expense={expense} onUpdate={handleUpdateExpense} onDelete={handleDeleteExpense} />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {totalPages > 1 && (
                  <Pagination currentPage={currentPage} totalPages={totalPages} onPageChange={setCurrentPage} totalItems={sortedExpenses.length} itemsPerPage={ITEMS_PER_PAGE} className="mt-4" />
                )}
              </>
            )}
          </Card>
        </div>
        <Card title="Spending by Category" isLoading={isLoading}>
          <div className="h-64"><PieChart data={getCategoryData()} height={250} /></div>
        </Card>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card title="Monthly Spending Trend" isLoading={isLoading}>
          <BarChart data={getMonthlySpendingData()} bars={[{ dataKey: 'value', name: 'Amount', color: '#3867D6' }]} height={300} />
        </Card>
        <Card title="Payment Methods" isLoading={isLoading}>
          <PieChart data={getPaymentMethodData()} height={300} />
        </Card>
      </div>

      <Dialog isOpen={isAddDialogOpen} onClose={() => setIsAddDialogOpen(false)} title="Add Expense">
        <ExpenseForm key={`add-expense-form-${formKey}`} onSubmit={handleAddExpense} onCancel={() => setIsAddDialogOpen(false)} isLoading={isSubmitting} />
      </Dialog>

      <Dialog isOpen={isBulkEditOpen} onClose={() => setIsBulkEditOpen(false)} title={`Edit ${selectedIds.size} Expense(s)`}>
        <div className="space-y-4">
          <Select label="Category" value={bulkCategory} onChange={(e) => setBulkCategory(e.target.value)} options={[{ value: '', label: 'Keep current' }, ...EXPENSE_CATEGORIES.map(cat => ({ value: cat, label: cat }))]} fullWidth />
          <Select label="Payment Method" value={bulkPayment} onChange={(e) => setBulkPayment(e.target.value)} options={[{ value: '', label: 'Keep current' }, ...PAYMENT_METHODS.map(m => ({ value: m, label: m }))]} fullWidth />
          <div className="flex justify-end gap-2 pt-2">
            <Button variant="outline" onClick={() => setIsBulkEditOpen(false)}>Cancel</Button>
            <Button onClick={handleBulkEdit} isLoading={isBulkLoading}>Apply Changes</Button>
          </div>
        </div>
      </Dialog>

      <ExpenseImportDialog isOpen={isImportDialogOpen} onClose={handleImportClose} />
    </div>
  );
};
