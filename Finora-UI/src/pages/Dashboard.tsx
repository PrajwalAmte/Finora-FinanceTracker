import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { TrendingUp, CreditCard, DollarSign, BarChart4, ArrowRight } from 'lucide-react';
import { DashboardMetricCard } from '../components/dashboard/DashboardMetricCard';
import { Card } from '../components/ui/Card';
import { PieChart } from '../components/charts/PieChart';
import { Button } from '../components/ui/Button';
import { summaryApi, ComprehensiveFinanceSummary } from '../api/summaryApi';

export const Dashboard: React.FC = () => {
  const [isLoading, setIsLoading] = useState(true);
  const [summary, setSummary] = useState<ComprehensiveFinanceSummary | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setIsLoading(true);
        const today = new Date();
        const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1).toISOString().split('T')[0];
        const lastDayOfMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0).toISOString().split('T')[0];
        const result = await summaryApi.getComprehensiveSummary(firstDayOfMonth, lastDayOfMonth);
        setSummary(result);
      } catch {
        // Error handled by apiClient interceptor
      } finally {
        setIsLoading(false);
      }
    };
    fetchData();
  }, []);

  const getExpenseCategoryData = () => {
    if (!summary?.expenseSummary?.expensesByCategory) return [];
    return Object.entries(summary.expenseSummary.expensesByCategory).map(([name, value]) => ({
      name,
      value
    }));
  };

  const getAssetAllocationData = () => {
    const investmentValue = summary?.investmentSummary?.totalValue || 0;
    const sipValue = summary?.sipSummary?.totalCurrentValue || 0;
    return [
      { name: 'Investments', value: investmentValue },
      { name: 'SIPs', value: sipValue }
    ];
  };

  const getTotalReturnPercentage = () => {
    if (!summary) return undefined;
    const investmentValue = summary.investmentSummary.totalValue || 0;
    const investmentProfitLoss = summary.investmentSummary.totalProfitLoss || 0;
    const sipValue = summary.sipSummary.totalCurrentValue || 0;
    const sipProfitLoss = summary.sipSummary.totalProfitLoss || 0;
    const totalCurrentValue = investmentValue + sipValue;
    const totalProfitLoss = investmentProfitLoss + sipProfitLoss;
    const totalInvested = totalCurrentValue - totalProfitLoss;
    if (totalInvested === 0) return undefined;
  
  return (totalProfitLoss / totalInvested) * 100;
};

  return (
    <div className="animate-fade-in">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
        {/* Net Worth */}
        <DashboardMetricCard
          title="Net Worth"
          value={summary?.netWorth || 0}
          icon={<TrendingUp />}
          isLoading={isLoading}
          onClick={() => {}}
        />
        
        {/* Total Investments */}
        <DashboardMetricCard
          title="Investments"
          value={summary?.totalAssets || 0}
          changeValue={getTotalReturnPercentage()}
          changeLabel="Overall Return"
          icon={<TrendingUp />}
          isLoading={isLoading}
          onClick={() => {}}
        />
        
        {/* Total Loans */}
        <DashboardMetricCard
          title="Loan Balance"
          value={summary?.loanSummary?.totalBalance || 0}
          icon={<CreditCard />}
          isLoading={isLoading}
          onClick={() => {}}
        />
        
        {/* Monthly Expenses */}
        <DashboardMetricCard
          title="Monthly Expenses"
          value={summary?.expenseSummary?.totalExpenses || 0}
          icon={<DollarSign />}
          isLoading={isLoading}
          onClick={() => {}}
        />
      </div>
      
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Asset Allocation Chart */}
        <Card
          title="Asset Allocation"
          isLoading={isLoading}
          footer={
            <div className="flex justify-end">
              <Button 
                variant="ghost" 
                size="sm"
                iconRight={<ArrowRight size={16} />}
                onClick={() => {}}
              >
                <Link to="/investments">View Investments</Link>
              </Button>
            </div>
          }
        >
          <PieChart
            data={getAssetAllocationData()}
            height={300}
          />
        </Card>
        
        {/* Expense Breakdown Chart */}
        <Card
          title="Expense Breakdown"
          isLoading={isLoading}
          footer={
            <div className="flex justify-end">
              <Button 
                variant="ghost" 
                size="sm"
                iconRight={<ArrowRight size={16} />}
                onClick={() => {}}
              >
                <Link to="/expenses">View Expenses</Link>
              </Button>
            </div>
          }
        >
          <PieChart
            data={getExpenseCategoryData()}
            height={300}
          />
        </Card>
      </div>
    </div>
  );
};