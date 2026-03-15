import React, { useState, useEffect } from 'react';
import { Plus, TrendingUp, Download, Search, Upload, RefreshCw, PieChart as PieChartIcon } from 'lucide-react';
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
import { investmentApi } from '../api/investmentApi';
import { formatCurrency, formatDate, formatPercentage, getStatusColorClass } from '../utils/formatters';
import { PieChart } from '../components/charts/PieChart';
import { generateInvestmentReport } from '../utils/excel-generator';
import { toast } from '../utils/notifications';

const ITEMS_PER_PAGE = 10;

export const InvestmentsPage: React.FC = () => {
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

  const totalPages = Math.ceil(filteredInvestments.length / ITEMS_PER_PAGE);
  const paginatedInvestments = filteredInvestments.slice(
    (currentPage - 1) * ITEMS_PER_PAGE,
    currentPage * ITEMS_PER_PAGE
  );

  const getInvestmentsByType = () => {
    const groupedData: Record<string, number> = {};
    
    investments.forEach(investment => {
      const type = investment.type;
      const currentValue = investment.currentValue || (investment.quantity * investment.currentPrice);
      
      if (groupedData[type]) {
        groupedData[type] += currentValue;
      } else {
        groupedData[type] = currentValue;
      }
    });
    
    return Object.entries(groupedData).map(([name, value]) => ({
      name,
      value
    }));
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
      // Backend runs async; wait then reload
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
    generateInvestmentReport(
      filteredInvestments,
      'Investment Report',
      summary?.totalValue,
      summary?.totalProfitLoss
    );
  };

  return (
    <div>
      {error && (
        <div className="mb-4 px-4 py-3 rounded-md bg-red-50 text-red-700 border border-red-200">
          {error}
        </div>
      )}
      <div className="flex flex-col sm:flex-row justify-between gap-4 mb-6">
        <div className="w-full sm:w-72">
          <Input
            placeholder="Search investments..."
            icon={<Search size={18} />}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            fullWidth
          />
        </div>
        
        <div className="flex gap-2">
          <Button
            variant="outline"
            iconLeft={<RefreshCw size={18} className={isRefreshing ? 'animate-spin' : ''} />}
            onClick={handleRefreshPrices}
            disabled={isRefreshing}
          >
            {isRefreshing ? 'Refreshing…' : 'Refresh Prices'}
          </Button>
          <Button
            variant="outline"
            iconLeft={<Download size={18} />}
            onClick={handleExportExcel}
          >
            Export
          </Button>
          <Button
            variant="outline"
            iconLeft={<Upload size={18} />}
            onClick={() => setIsImportDialogOpen(true)}
          >
            Import Statement
          </Button>
          <Button
            iconLeft={<Plus size={18} />}
            onClick={() => {
              setFormKey(prev => prev + 1);
              setIsAddDialogOpen(true);
            }}
          >
            Add Investment
          </Button>
        </div>
      </div>
      
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
        <Card className="h-full" isLoading={isLoading}>
          <div className="text-lg font-medium text-neutral-900 dark:text-white mb-1">Total Value</div>
          <div className="text-3xl font-bold text-primary-600">{formatCurrency(summary?.totalValue || 0)}</div>
        </Card>
        <Card className="h-full" isLoading={isLoading}>
          <div className="text-lg font-medium text-neutral-900 dark:text-white mb-1">Total Profit/Loss</div>
          <div className={`text-3xl font-bold ${getStatusColorClass(summary?.totalProfitLoss || 0)}`}>
            {formatCurrency(summary?.totalProfitLoss || 0)}
          </div>
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
                    <Button
                      variant="outline"
                      iconLeft={<Upload size={18} />}
                      onClick={() => setIsImportDialogOpen(true)}
                    >
                      Import Statement
                    </Button>
                    <Button 
                      variant="primary" 
                      iconLeft={<Plus size={18} />}
                      onClick={() => {
                        setFormKey(prev => prev + 1);
                        setIsAddDialogOpen(true);
                      }}
                    >
                      Add Manually
                    </Button>
                  </div>
                }
              />
            ) : (
              <>
                <div className="overflow-x-auto">
                  <table className="w-full divide-y divide-neutral-200 dark:divide-neutral-700">
                    <thead>
                      <tr>
                        <th className="px-4 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Investment</th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Price</th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Quantity</th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Value</th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">P&L</th>
                        <th className="px-4 py-3 text-right text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Return %</th>
                        <th className="px-4 py-3 text-center text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-neutral-200 dark:divide-neutral-700">
                      {paginatedInvestments.map((investment) => (
                        <tr 
                          key={investment.id} 
                          className="hover:bg-neutral-50 dark:hover:bg-neutral-800 transition-colors"
                        >
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
                                    <Badge 
                                      size="sm"
                                      variant={
                                        investment.importSource === 'CAS' ? 'default' :
                                        investment.importSource === 'CAMS' ? 'secondary' :
                                        'outline'
                                      }
                                    >
                                      {investment.importSource === 'ZERODHA_EXCEL' ? 'Zerodha' : investment.importSource}
                                    </Badge>
                                  )}
                                </div>
                              </div>
                            </div>
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-right">
                            {formatCurrency(investment.currentPrice)}
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-right">
                            {investment.quantity}
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-right font-medium">
                            {formatCurrency(investment.currentValue || (investment.quantity * investment.currentPrice))}
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-right">
                            <span className={getStatusColorClass(investment.profitLoss || 0)}>
                              {formatCurrency(investment.profitLoss || 0)}
                            </span>
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap text-sm text-right">
                            <span className={getStatusColorClass(investment.returnPercentage || 0)}>
                              {formatPercentage(investment.returnPercentage || 0)}
                            </span>
                          </td>
                          <td className="px-4 py-4 whitespace-nowrap text-center">
                            <InvestmentActions
                              investment={investment}
                              onUpdate={handleUpdateInvestment}
                              onDelete={handleDeleteInvestment}
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
                    totalItems={filteredInvestments.length}
                    itemsPerPage={ITEMS_PER_PAGE}
                    className="mt-4"
                  />
                )}
              </>
            )}
          </Card>
        </div>
        
        <Card title="Allocation by Type" isLoading={isLoading}>
          <PieChart data={getInvestmentsByType()} />
        </Card>
      </div>

      <Dialog
        isOpen={isAddDialogOpen}
        onClose={() => setIsAddDialogOpen(false)}
        title="Add Investment"
      >
        <InvestmentForm
          key={`add-investment-form-${formKey}`}
          onSubmit={handleAddInvestment}
          onCancel={() => setIsAddDialogOpen(false)}
          isLoading={isSubmitting}
        />
      </Dialog>

      <StatementUploadDialog
        isOpen={isImportDialogOpen}
        onClose={(success) => {
          setIsImportDialogOpen(false);
          if (success) {
            loadInvestments();
          }
        }}
      />
    </div>
  );
};