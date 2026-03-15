import apiClient from './apiClient';

export interface ComprehensiveFinanceSummary {
  expenseSummary: {
    totalExpenses: number;
    expensesByCategory: Record<string, number>;
  };
  investmentSummary: {
    totalValue: number;
    totalProfitLoss: number;
  };
  loanSummary: {
    totalBalance: number;
  };
  sipSummary: {
    totalInvestment: number;
    totalCurrentValue: number;
    totalProfitLoss: number;
  };
  totalAssets: number;
  totalLiabilities: number;
  netWorth: number;
  averageMonthlyExpense: number;
}

const BASE_PATH = '/finance-summary';

export const summaryApi = {
  getComprehensiveSummary: async (startDate?: string, endDate?: string): Promise<ComprehensiveFinanceSummary> => {
    const response = await apiClient.get(BASE_PATH, {
      params: { startDate, endDate },
    });
    return response.data;
  }
};