import React, { useState, useEffect } from 'react';
import { Plus, LineChart, Download, Search } from 'lucide-react';
import { Card } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Dialog } from '../components/ui/Dialog';
import { SipForm } from '../components/forms/SipForm';
import { SipActions } from '../components/forms/SipActions';
import { Sip, SipSummary } from '../types/Sip';
import { sipApi } from '../api/sipApi';
import { formatCurrency, formatDate, getStatusColorClass, formatPercentage } from '../utils/formatters';
import { PieChart } from '../components/charts/PieChart';
import { generateSipReport } from '../utils/excel-generator';

export const SipsPage: React.FC = () => {
  const [sips, setSips] = useState<Sip[]>([]);
  const [summary, setSummary] = useState<SipSummary | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [isAddDialogOpen, setIsAddDialogOpen] = useState(false);
  const [formKey, setFormKey] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  useEffect(() => {
    const loadSips = async () => {
      try {
        setIsLoading(true);
        const [sipsData, summaryData] = await Promise.all([
          sipApi.getAll(),
          sipApi.getSummary()
        ]);
        setSips(sipsData);
        setSummary(summaryData);
      } catch (error) {
        console.error('Failed to load SIPs:', error);
      } finally {
        setIsLoading(false);
      }
    };
    
    loadSips();
  }, []);
  
  // Filter SIPs based on search term
  const filteredSips = sips.filter(sip => 
    sip.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    sip.schemeCode.toLowerCase().includes(searchTerm.toLowerCase())
  );
  
  // Calculate return percentage
  const calculateReturnPercentage = (invested: number, currentValue: number): number => {
    if (invested === 0) return 0;
    return ((currentValue - invested) / invested) * 100;
  };
  
  // Group SIPs by invested amount for chart
  const getSipData = () => {
    return filteredSips.map(sip => ({
      name: sip.name,
      value: sip.currentValue || 0
    }));
  };
  
  const handleAddSip = async (data: Omit<Sip, 'id'>) => {
    try {
      setIsSubmitting(true);
      const newSip = await sipApi.create(data);
      setSips(prev => [...prev, newSip]);
      setIsAddDialogOpen(false);
    } catch (error) {
      console.error('Failed to add SIP:', error);
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUpdateSip = (updatedSip: Sip) => {
    setSips(prev => prev.map(sip => 
      sip.id === updatedSip.id ? updatedSip : sip
    ));
  };

  const handleDeleteSip = (id: number) => {
    setSips(prev => prev.filter(sip => sip.id !== id));
  };

  // Handle export to Excel
  const handleExportExcel = () => {
    generateSipReport(
      filteredSips,
      'SIP Report',
      summary?.totalInvestment,
      summary?.totalCurrentValue,
      summary?.totalProfitLoss
    );
  };

  return (
    <div>
      <div className="flex flex-col sm:flex-row justify-between gap-4 mb-6">
        <div className="w-full sm:w-72">
          <Input
            placeholder="Search SIPs..."
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
            Add SIP
          </Button>
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
              <div className="text-center py-6">
                <p className="text-neutral-500 dark:text-neutral-400">No SIPs found</p>
                <Button 
                  className="mt-3" 
                  variant="primary" 
                  iconLeft={<Plus size={18} />}
                  onClick={() => {
              setFormKey(prev => prev + 1);
              setIsAddDialogOpen(true);
            }}
                >
                  Add SIP
                </Button>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full divide-y divide-neutral-200 dark:divide-neutral-700">
                  <thead>
                    <tr>
                      <th className="px-4 py-3 text-left text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">SIP Name</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Monthly Amount</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">NAV</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Units</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Invested</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Current Value</th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Return %</th>
                      <th className="px-4 py-3 text-center text-xs font-medium text-neutral-500 dark:text-neutral-400 uppercase tracking-wider">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-neutral-200 dark:divide-neutral-700">
                    {filteredSips.map((sip) => (
                      <tr 
                        key={sip.id} 
                        className="hover:bg-neutral-50 dark:hover:bg-neutral-800 transition-colors"
                      >
                        <td className="px-4 py-4 whitespace-nowrap">
                          <div className="flex items-start">
                            <div className="flex-shrink-0 h-8 w-8 flex items-center justify-center rounded-full bg-primary-100 dark:bg-primary-900">
                              <LineChart size={16} className="text-primary-600 dark:text-primary-400" />
                            </div>
                            <div className="ml-3">
                              <div className="text-sm font-medium text-neutral-900 dark:text-white">{sip.name}</div>
                              <div className="text-xs text-neutral-500 dark:text-neutral-400">Started {formatDate(sip.startDate)}</div>
                            </div>
                          </div>
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap text-sm text-right">
                          {formatCurrency(sip.monthlyAmount)}
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap text-sm text-right">
                          {formatCurrency(sip.currentNav)}
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap text-sm text-right">
                          {sip.totalUnits.toFixed(2)}
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap text-sm text-right">
                          {formatCurrency(sip.totalInvested || 0)}
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap text-sm text-right font-medium">
                          {formatCurrency(sip.currentValue || 0)}
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap text-sm text-right">
                          {sip.totalInvested && sip.currentValue && (
                            <span className={getStatusColorClass((sip.currentValue - sip.totalInvested))}>
                              {formatPercentage(calculateReturnPercentage(sip.totalInvested, sip.currentValue))}
                            </span>
                          )}
                        </td>
                        <td className="px-4 py-4 whitespace-nowrap text-center">
                          <SipActions
                            sip={sip}
                            onUpdate={handleUpdateSip}
                            onDelete={handleDeleteSip}
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
        
        <Card title="SIP Allocation" isLoading={isLoading}>
          <div className="h-64">
            <PieChart
              data={getSipData()}
              height={250}
            />
          </div>
        </Card>
      </div>

      {/* Add SIP Dialog */}
      <Dialog
        isOpen={isAddDialogOpen}
        onClose={() => setIsAddDialogOpen(false)}
        title="Add SIP"
      >
        <SipForm
          key={`add-sip-form-${formKey}`}
          onSubmit={handleAddSip}
          onCancel={() => setIsAddDialogOpen(false)}
          isLoading={isSubmitting}
        />
      </Dialog>
    </div>
  );
};