import React, { useState, useEffect } from 'react';
import { Plus, Download, Calendar, Search, Receipt } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Select } from '../components/ui/Select';
import { Badge } from '../components/ui/Badge';
import { Dialog } from '../components/ui/Dialog';
import { ExpenseForm } from '../components/forms/ExpenseForm';
import { ExpenseActions } from '../components/forms/ExpenseActions';
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
  const [formKey, setFormKey] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
    if (startDate && endDate) {
      loadExpenses();
    }
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

  const totalPages = Math.ceil(filteredExpenses.length / ITEMS_PER_PAGE);
  const paginatedExpenses = filteredExpenses.slice(
    (currentPage - 1) * ITEMS_PER_PAGE,
    currentPage * ITEMS_PER_PAGE
  );

  const getCategoryData = () => {
    const categories: Record<string, number> = {};
    
    filteredExpenses.forEach(expense => {
      if (categories[expense.category]) {
        categories[expense.category] += expense.amount;
      } else {
        categories[expense.category] = expense.amount;
      }
    });
    
    return Object.entries(categories).map(([name, value]) => ({ name, value }));
  };

  const getPaymentMethodData = () => {
    const methods: Record<string, number> = {};
    
    filteredExpenses.forEach(expense => {
      if (methods[expense.paymentMethod]) {
        methods[expense.paymentMethod] += expense.amount;
      } else {
        methods[expense.paymentMethod] = expense.amount;
      }
    });
    
    return Object.entries(methods).map(([name, value]) => ({ name, value }));
  };

  const getMonthlySpendingData = () => {
    const months: Record<string, number> = {};
    
    // Initialize with last 6 months
    const today = new Date();
    for (let i = 5; i >= 0; i--) {
      const date = new Date(today.getFullYear(), today.getMonth() - i, 1);
      const monthKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
      months[monthKey] = 0;
    }
    
    // Group expenses by month
    filteredExpenses.forEach(expense => {
      const date = new Date(expense.date);
      const monthKey = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
      
      if (months[monthKey] !== undefined) {
        months[monthKey] += expense.amount;
      }
    });
    
    // Format for chart
    return Object.entries(months).map(([key, value]) => {
      const [year, month] = key.split('-');
      const date = new Date(parseInt(year), parseInt(month) - 1, 1);
      return {
        name: date.toLocaleDateString('en-US', { month: 'short', year: '2-digit' }),
        value: value
      };
    });
  };

  const handleExportExcel = () => {
    generateExpenseReport(
      filteredExpenses,
      'Expense Report',
      startDate,
      endDate,
      summary?.totalExpenses
    );
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
    setExpenses(prev => prev.map(expense => 
      expense.id === updatedExpense.id ? updatedExpense : expense
    ));
  };

  const handleDeleteExpense = (id: number) => {
    setExpenses(prev => prev.filter(expense => expense.id !== id));
  };

  return (
    <div>
      {error && (
        <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 border border-red-200">
          {error}
        </div>
      )}
      <div className="grid grid-cols-1 sm:grid-cols-12 gap-4 mb-6">
        <div className="sm:col-span-3">
          <Input
            type="date"
            label="Start Date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            icon={<Calendar size={18} />}
            fullWidth
          />
        </div>
        
        <div className="sm:col-span-3">
          <Input
            type="date"
            label="End Date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            icon={<Calendar size={18} />}
            fullWidth
          />
        </div>
        
        <div className="sm:col-span-3">
          <Select
            label="Category"
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            options={[
              { value: '', label: 'All Categories' },
              ...EXPENSE_CATEGORIES.map(cat => ({ value: cat, label: cat }))
            ]}
            fullWidth
          />
        </div>
        
        <div className="sm:col-span-3 flex items-end gap-2">
          <Button
            variant="outline"
            iconLeft={<Download size={18} />}
            onClick={handleExportExcel}
            fullWidth
          >
            Export
          </Button>
          <Button
            iconLeft={<Plus size={18} />}
            fullWidth
            onClick={() => {
              setFormKey(prev => prev + 1);
              setIsAddDialogOpen(true);
            }}
          >
            Add Expense
          </Button>
        </div>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Card className="h-full" isLoading={isLoading}>
          <div className="text-lg font-medium text-neutral-900 dark:text-white mb-1">Total Expenses</div>
          <div className="text-3xl font-bold text-error-600">{formatCurrency(summary?.totalExpenses || 0)}</div>
        </Card>
        <Card className="h-full" isLoading={isLoading}>
          <div className="text-lg font-medium text-neutral-900 dark:text-white mb-1">Categories</div>
          <div className="text-3xl font-bold text-neutral-800 dark:text-white">
            {summary?.expensesByCategory ? Object.keys(summary.expensesByCategory).length : 0}
          </div>
        </Card>
        <Card className="h-full" isLoading={isLoading}>
          <div className="text-lg font-medium text-neutral-900 dark:text-white mb-1">Transactions</div>
          <div className="text-3xl font-bold text-neutral-800 dark:text-white">{filteredExpenses.length}</div>
        </Card>
      </div>
      
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        <div className="lg:col-span-2">
          <Card title="Expense Transactions" isLoading={isLoading}>
            <div className="mb-4">
              <Input
                placeholder="Search expenses..."
                icon={<Search size={18} />}
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                fullWidth
              />
            </div>
            
            {filteredExpenses.length === 0 ? (
              <EmptyState
                icon={<Receipt size={48} />}
                title="No expenses found"
                description="Start tracking your spending by adding your first expense"
                action={
                  <Button 
                    variant="primary" 
                    iconLeft={<Plus size={18} />}
                    onClick={() => {
                      setFormKey(prev => prev + 1);
                      setIsAddDialogOpen(true);
                    }}
                  >
                    Add Expense
                  </Button>
                }
              />
            ) : (
              <>
                <div className="overflow-x-auto">
                  <table className="w-full divide-y divide-neutral-200 dark:divide-neutral-700">
                    <thead>
                      <tr>
                        <th className="px-4 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Description</th>
                        <th className="px-4 py-3 text-center text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Date</th>
                        <th className="px-4 py-3 text-center text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Category</th>
                        <th className="px-4 py-3 text-center text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Payment</th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Amount</th>
                        <th className="px-4 py-3 text-center text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-200 dark:divide-neutral-700">
                      {paginatedExpenses.map((expense) => (
                        <tr 
                          key={expense.id} 
                          className="hover:bg-neutral-50 dark:hover:bg-neutral-800 transition-colors"
                        >
                          <td className="px-4 py-4 whitespace-nowrap">
                            <div className="text-sm font-medium text-neutral-900 dark:text-white">{expense.description}</div>
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-center text-neutral-500 dark:text-neutral-400">
                            {formatDate(expense.date, 'MMM dd, yyyy')}
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap text-center">
                            <Badge variant="primary" size="sm">{expense.category}</Badge>
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-center text-neutral-500 dark:text-neutral-400">
                            {expense.paymentMethod}
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-right font-medium text-error-600 dark:text-error-400">
                            {formatCurrency(expense.amount)}
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap text-center">
                            <ExpenseActions
                              expense={expense}
                              onUpdate={handleUpdateExpense}
                              onDelete={handleDeleteExpense}
                            />
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {totalPages > 1 && (
                  <Pagination
                    currentPage={currentPage}
                    totalPages={totalPages}
                    onPageChange={setCurrentPage}
                    totalItems={filteredExpenses.length}
                    itemsPerPage={ITEMS_PER_PAGE}
                    className="mt-4"
                  />
                )}
              </>
            )}
          </Card>
        </div>
        
        <Card title="Spending by Category" isLoading={isLoading}>
          <div className="h-64">
            <PieChart
              data={getCategoryData()}
              height={250}
            />
          </div>
        </Card>
      </div>
      
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card title="Monthly Spending Trend" isLoading={isLoading}>
          <BarChart
            data={getMonthlySpendingData()}
            bars={[
              { dataKey: 'value', name: 'Amount', color: '#3867D6' }
            ]}
            height={300}
          />
        </Card>
        
        <Card title="Payment Methods" isLoading={isLoading}>
          <PieChart
            data={getPaymentMethodData()}
            height={300}
          />
        </Card>
      </div>
      
      {/* Add Expense Dialog */}
      <Dialog
        isOpen={isAddDialogOpen}
        onClose={() => setIsAddDialogOpen(false)}
        title="Add Expense"
      >
        <ExpenseForm
          key={`add-expense-form-${formKey}`}
          onSubmit={handleAddExpense}
          onCancel={() => setIsAddDialogOpen(false)}
          isLoading={isSubmitting}
        />
      </Dialog>
    </div>
  );
};