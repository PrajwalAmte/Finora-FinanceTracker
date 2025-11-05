import React, { useState, useEffect } from 'react';
import { Plus, CreditCard, Download, Search } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Badge } from '../components/ui/Badge';
import { Dialog } from '../components/ui/Dialog';
import { LoanForm } from '../components/forms/LoanForm';
import { LoanActions } from '../components/forms/LoanActions';
import { Loan, LoanSummary } from '../types/Loan';
import { loanApi } from '../api/loanApi';
import { formatCurrency, formatDate } from '../utils/formatters';
import { PieChart } from '../components/charts/PieChart';
import { BarChart } from '../components/charts/BarChart';
import { generateLoanReport } from '../utils/excel-generator';

export const LoansPage: React.FC = () => {
  const [loans, setLoans] = useState<Loan[]>([]);
  const [summary, setSummary] = useState<LoanSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [formKey, setFormKey] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  
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
      } catch (error) {
        console.error('Failed to load loans:', error);
      } finally {
        setIsLoading(false);
      }
    };
    
    loadLoans();
  }, []);
  
  // Filter loans based on search term
  const filteredLoans = loans.filter(loan => 
    loan.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    loan.interestType.toLowerCase().includes(searchTerm.toLowerCase())
  );
  
  // Get chart data for loan distribution
  const getLoanDistributionData = () => {
    return filteredLoans.map(loan => ({
      name: loan.name,
      value: loan.currentBalance
    }));
  };
  
  // Get interest type distribution
  const getInterestTypeData = () => {
    const data: Record<string, number> = {};
    
    filteredLoans.forEach(loan => {
      const type = loan.interestType;
      if (data[type]) {
        data[type] += loan.currentBalance;
      } else {
        data[type] = loan.currentBalance;
      }
    });
    
    return Object.entries(data).map(([name, value]) => ({ name, value }));
  };
  
  // Get bar chart data for principal vs interest
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
    } catch (error) {
      console.error('Failed to add loan:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUpdateLoan = (updatedLoan: Loan) => {
    setLoans(prev => prev.map(loan => 
      loan.id === updatedLoan.id ? updatedLoan : loan
    ));
  };

  const handleDeleteLoan = (id: number) => {
    setLoans(prev => prev.filter(loan => loan.id !== id));
  };

  // Handle export to Excel
  const handleExportExcel = () => {
    generateLoanReport(
      filteredLoans,
      'Loan Report',
      summary?.totalBalance
    );
  };

  return (
    <div>
      <div className="flex flex-col sm:flex-row justify-between gap-4 mb-6">
        <div className="w-full sm:w-72">
          <Input
            placeholder="Search loans..."
            icon={<Search size={18} />}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            fullWidth
          />
        </div>
        
        <div className="flex gap-2">
          <Button
            variant="outline"
            iconLeft={<Download size={18} />}
            onClick={handleExportExcel}
          >
            Export
          </Button>
          <Button
            iconLeft={<Plus size={18} />}
            onClick={() => {
              setFormKey(prev => prev + 1);
              setIsAddDialogOpen(true);
            }}
          >
            Add Loan
          </Button>
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
            {loans.length > 0 
              ? (loans.reduce((sum, loan) => sum + loan.interestRate, 0) / loans.length).toFixed(2)
              : 0
            }%
          </div>
        </Card>
      </div>
      
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        <div className="lg:col-span-2">
          <Card title="Loan Portfolio" isLoading={isLoading}>
            {filteredLoans.length === 0 ? (
              <div className="text-center py-6">
                <p className="text-neutral-500 dark:text-neutral-400">No loans found</p>
                <Button 
                  className="mt-3" 
                  variant="primary" 
                  iconLeft={<Plus size={18} />}
                  onClick={() => {
              setFormKey(prev => prev + 1);
              setIsAddDialogOpen(true);
            }}
                >
                  Add Loan
                </Button>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full divide-y divide-neutral-200 dark:divide-neutral-700">
                  <thead>
                    <tr>
                      <th className="px-4 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Loan</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Principal</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Interest Rate</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">EMI</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Start Date</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Remaining</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Balance</th>
                      <th className="px-4 py-3 text-center text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-200 dark:divide-neutral-700">
                    {filteredLoans.map((loan) => (
                      <tr 
                        key={loan.id} 
                        className="hover:bg-neutral-50 dark:hover:bg-neutral-800 transition-colors"
                      >
                        <td className="px-4 py-4 whitespace-nowrap">
                          <div className="flex items-start">
                            <div className="flex-shrink-0 h-8 w-8 flex items-center justify-center rounded-full bg-error-100 dark:bg-error-900">
                              <CreditCard size={16} className="text-error-600 dark:text-error-400" />
                            </div>
                            <div className="ml-3">
                              <div className="text-sm font-medium text-neutral-900 dark:text-white">{loan.name}</div>
                              <div className="flex items-center">
                                <Badge 
                                  variant={loan.interestType === 'SIMPLE' ? 'primary' : 'warning'} 
                                  size="sm"
                                >
                                  {loan.interestType}
                                </Badge>
                                {loan.compoundingFrequency && (
                                  <span className="text-xs text-neutral-500 dark:text-neutral-400 ml-2">
                                    {loan.compoundingFrequency}
                                  </span>
                                )}
                              </div>
                            </div>
                          </div>
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap text-sm text-right">
                          {formatCurrency(loan.principalAmount)}
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap text-sm text-right">
                          {loan.interestRate}%
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap text-sm text-right">
                          {formatCurrency(loan.emiAmount || 0)}
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap text-sm text-right">
                          {formatDate(loan.startDate)}
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap text-sm text-right">
                          {loan.remainingMonths || 0} months
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap text-sm text-right font-medium">
                          {formatCurrency(loan.currentBalance)}
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap text-center">
                          <LoanActions
                            loan={loan}
                            onUpdate={handleUpdateLoan}
                            onDelete={handleDeleteLoan}
                          />
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </div>
        
        <Card title="Loan Distribution" isLoading={isLoading}>
          <div className="h-64">
            <PieChart
              data={getLoanDistributionData()}
              height={250}
            />
          </div>
        </Card>
      </div>
      
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card title="Principal vs Interest" isLoading={isLoading}>
          <BarChart
            data={getPrincipalInterestData()}
            bars={[
              { dataKey: 'principal', name: 'Principal', color: '#3867D6' },
              { dataKey: 'interest', name: 'Interest', color: '#E53E3E' }
            ]}
            stacked={true}
            height={300}
          />
        </Card>
        
        <Card title="Interest Type Distribution" isLoading={isLoading}>
          <PieChart
            data={getInterestTypeData()}
            height={300}
          />
        </Card>
      </div>

      {/* Add Loan Dialog */}
      <Dialog
        isOpen={isAddDialogOpen}
        onClose={() => setIsAddDialogOpen(false)}
        title="Add Loan"
      >
        <LoanForm
          key={`add-loan-form-${formKey}`}
          onSubmit={handleAddLoan}
          onCancel={() => setIsAddDialogOpen(false)}
          isLoading={isSubmitting}
        />
      </Dialog>
    </div>
  );
};