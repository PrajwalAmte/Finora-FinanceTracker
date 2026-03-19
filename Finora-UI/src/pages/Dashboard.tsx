import React, { useState, useEffect, useRef } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { TrendingUp, CreditCard, DollarSign, ArrowRight } from 'lucide-react';
import { DashboardMetricCard } from '../components/dashboard/DashboardMetricCard';
import { Card } from '../components/ui/Card';
import { PieChart } from '../components/charts/PieChart';
import { Button } from '../components/ui/Button';
import { DateRangePicker, DateRange, DatePreset } from '../components/ui/DateRangePicker';
import { WelcomeOnboarding, QuickActions } from '../components/dashboard/WelcomeOnboarding';
import { ComprehensiveFinanceSummary } from '../api/summaryApi';
import { useSummaryApi } from '../utils/data-context';

const DASHBOARD_CACHE_KEY = 'dashboard_summary';

export const Dashboard: React.FC = () => {
  const navigate = useNavigate();
  const summaryApi = useSummaryApi();
  const [isLoading, setIsLoading] = useState(true);
  const [summary, setSummary] = useState<ComprehensiveFinanceSummary | null>(null);
  const [showOnboarding, setShowOnboarding] = useState(true);
  const fetchId = useRef(0);
  const [dateRange, setDateRange] = useState<DateRange>(() => {
    const today = new Date();
    return {
      startDate: new Date(today.getFullYear(), today.getMonth(), 1).toISOString().split('T')[0],
      endDate: new Date(today.getFullYear(), today.getMonth() + 1, 0).toISOString().split('T')[0],
      preset: 'thisMonth' as DatePreset
    };
  });

  useEffect(() => {
    const currentFetchId = ++fetchId.current;
    const dateKey = `${dateRange.startDate}_${dateRange.endDate}`;
    let hasCachedData = false;

    try {
      const cached = sessionStorage.getItem(DASHBOARD_CACHE_KEY);
      if (cached) {
        const parsed = JSON.parse(cached);
        if (parsed.dateKey === dateKey) {
          setSummary(parsed.data);
          setIsLoading(false);
          hasCachedData = true;
        }
      }
    } catch {
      // ignore parse errors
    }

    if (!hasCachedData) {
      setIsLoading(true);
    }

    const fetchData = async () => {
      try {
        const result = await summaryApi.getComprehensiveSummary(dateRange.startDate, dateRange.endDate);
        if (fetchId.current !== currentFetchId) return;
        setSummary(result);
        try {
          sessionStorage.setItem(DASHBOARD_CACHE_KEY, JSON.stringify({ data: result, dateKey }));
        } catch {
          // ignore quota errors
        }
      } catch {
        // Error handled by apiClient interceptor
      } finally {
        if (fetchId.current === currentFetchId) {
          setIsLoading(false);
        }
      }
    };
    fetchData();
  }, [dateRange]);

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

  const hasData = summary && (
    (summary.expenseSummary?.totalExpenses || 0) > 0 ||
    (summary.investmentSummary?.totalValue || 0) > 0 ||
    (summary.sipSummary?.totalCurrentValue || 0) > 0 ||
    (summary.loanSummary?.totalBalance || 0) > 0
  );

  return (
    <div className="animate-fade-in space-y-6">
      {!isLoading && !hasData && showOnboarding && <WelcomeOnboarding hasData={!!hasData} onDismiss={() => setShowOnboarding(false)} />}
      
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Dashboard</h1>
        <div className="flex items-center gap-3">
          <DateRangePicker value={dateRange} onChange={setDateRange} />
          {!isLoading && !hasData && <QuickActions />}
        </div>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <DashboardMetricCard
          title="Net Worth"
          value={summary?.netWorth || 0}
          icon={<TrendingUp />}
          isLoading={isLoading}
          onClick={() => navigate('/investments')}
        />
        
        <DashboardMetricCard
          title="Investments"
          value={summary?.totalAssets || 0}
          changeValue={getTotalReturnPercentage()}
          changeLabel="Overall Return"
          icon={<TrendingUp />}
          isLoading={isLoading}
          onClick={() => navigate('/investments')}
        />
        
        <DashboardMetricCard
          title="Loan Balance"
          value={summary?.loanSummary?.totalBalance || 0}
          icon={<CreditCard />}
          isLoading={isLoading}
          onClick={() => navigate('/loans')}
        />
        
        <DashboardMetricCard
          title="Monthly Expenses"
          value={summary?.expenseSummary?.totalExpenses || 0}
          icon={<DollarSign />}
          isLoading={isLoading}
          onClick={() => navigate('/expenses')}
        />
      </div>
      
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card
          title="Asset Allocation"
          isLoading={isLoading}
          footer={
            <div className="flex justify-end">
              <Link to="/investments">
                <Button 
                  variant="ghost" 
                  size="sm"
                  iconRight={<ArrowRight size={16} />}
                >
                  View Investments
                </Button>
              </Link>
            </div>
          }
        >
          <PieChart
            data={getAssetAllocationData()}
            height={300}
          />
        </Card>
        
        <Card
          title="Expense Breakdown"
          isLoading={isLoading}
          footer={
            <div className="flex justify-end">
              <Link to="/expenses">
                <Button 
                  variant="ghost" 
                  size="sm"
                  iconRight={<ArrowRight size={16} />}
                >
                  View Expenses
                </Button>
              </Link>
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