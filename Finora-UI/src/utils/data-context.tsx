import { useLocalVault } from './local-vault-context';
import { expenseApi } from '../api/expenseApi';
import { investmentApi } from '../api/investmentApi';
import { loanApi } from '../api/loanApi';
import { sipApi } from '../api/sipApi';
import { summaryApi, ComprehensiveFinanceSummary } from '../api/summaryApi';
import { Expense, ExpenseSummary } from '../types/Expense';
import { Investment, InvestmentSummary } from '../types/Investment';
import { Loan, LoanSummary } from '../types/Loan';
import { Sip, SipSummary } from '../types/Sip';

let nextLocalId = Date.now();
function genId(): number {
  return ++nextLocalId;
}

export function useIsLocalMode(): boolean {
  const { isLocalMode } = useLocalVault();
  return isLocalMode;
}

export function useExpenseApi(): typeof expenseApi {
  const vault = useLocalVault();
  if (!vault.isLocalMode) return expenseApi;

  return {
    getAll: async () => vault.data.expenses,

    getById: async (id: number) => {
      const e = vault.data.expenses.find(x => x.id === id);
      if (!e) throw new Error('Expense not found');
      return e;
    },

    create: async (data: Omit<Expense, 'id'>) => {
      const newExpense: Expense = { ...data, id: genId() };
      vault.updateExpenses(prev => [...prev, newExpense]);
      return newExpense;
    },

    update: async (id: number, data: Partial<Expense>) => {
      let updated: Expense | null = null;
      vault.updateExpenses(prev => prev.map(e => {
        if (e.id === id) { updated = { ...e, ...data, id }; return updated!; }
        return e;
      }));
      return updated ?? { ...data, id } as Expense;
    },

    delete: async (id: number) => {
      vault.updateExpenses(prev => prev.filter(e => e.id !== id));
    },

    getByDateRange: async (startDate: string, endDate: string) => {
      return vault.data.expenses.filter(e => e.date >= startDate && e.date <= endDate);
    },

    getByCategory: async (category: string) => {
      return vault.data.expenses.filter(e => e.category === category);
    },

    getSummary: async (startDate?: string, endDate?: string): Promise<ExpenseSummary> => {
      const filtered = vault.data.expenses.filter(e =>
        (!startDate || e.date >= startDate) && (!endDate || e.date <= endDate)
      );
      const totalExpenses = filtered.reduce((sum, e) => sum + e.amount, 0);
      const byCategory: Record<string, number> = {};
      filtered.forEach(e => { byCategory[e.category] = (byCategory[e.category] || 0) + e.amount; });
      return { totalExpenses, expensesByCategory: byCategory };
    },

    getAverageMonthly: async () => {
      if (vault.data.expenses.length === 0) return 0;
      const months = new Set(vault.data.expenses.map(e => e.date.slice(0, 7)));
      const total = vault.data.expenses.reduce((s, e) => s + e.amount, 0);
      return months.size > 0 ? total / months.size : 0;
    },

    bulkDelete: async (ids: number[]) => {
      const set = new Set(ids);
      vault.updateExpenses(prev => prev.filter(e => !set.has(e.id!)));
      return { deleted: ids.length };
    },

    bulkUpdate: async (ids: number[], fields: { category?: string; paymentMethod?: string }) => {
      const set = new Set(ids);
      vault.updateExpenses(prev => prev.map(e => set.has(e.id!) ? { ...e, ...fields } : e));
      return { updated: ids.length };
    },
  };
}

export function useInvestmentApi(): typeof investmentApi {
  const vault = useLocalVault();
  if (!vault.isLocalMode) return investmentApi;

  const getValue = (inv: Investment) => inv.currentValue ?? inv.quantity * inv.currentPrice;
  const getPL = (inv: Investment) => {
    const costBasis = inv.quantity * inv.purchasePrice;
    return getValue(inv) - costBasis;
  };

  return {
    getAll: async () => vault.data.investments,

    getById: async (id: number) => {
      const inv = vault.data.investments.find(x => x.id === id);
      if (!inv) throw new Error('Investment not found');
      return inv;
    },

    create: async (data: Omit<Investment, 'id'>) => {
      const newInv: Investment = {
        ...data, id: genId(),
        currentValue: data.quantity * data.currentPrice,
        profitLoss: (data.quantity * data.currentPrice) - (data.quantity * data.purchasePrice),
        returnPercentage: data.purchasePrice > 0
          ? ((data.currentPrice - data.purchasePrice) / data.purchasePrice) * 100 : 0,
      };
      vault.updateInvestments(prev => [...prev, newInv]);
      return newInv;
    },

    update: async (id: number, data: Partial<Investment>) => {
      let updated: Investment | null = null;
      vault.updateInvestments(prev => prev.map(inv => {
        if (inv.id === id) {
          updated = { ...inv, ...data, id };
          updated.currentValue = updated.quantity * updated.currentPrice;
          updated.profitLoss = updated.currentValue - (updated.quantity * updated.purchasePrice);
          updated.returnPercentage = updated.purchasePrice > 0
            ? ((updated.currentPrice - updated.purchasePrice) / updated.purchasePrice) * 100 : 0;
          return updated;
        }
        return inv;
      }));
      return updated ?? { ...data, id } as Investment;
    },

    delete: async (id: number) => {
      vault.updateInvestments(prev => prev.filter(inv => inv.id !== id));
    },

    getSummary: async (): Promise<InvestmentSummary> => {
      const totalValue = vault.data.investments.reduce((s, inv) => s + getValue(inv), 0);
      const totalProfitLoss = vault.data.investments.reduce((s, inv) => s + getPL(inv), 0);
      return { totalValue, totalProfitLoss };
    },

    refreshPrices: async () => {
      throw new Error('Price refresh is not available in local vault mode');
    },

    searchMf: async () => [],

    addUnits: async (id: number, quantity: number, price: number) => {
      let updated: Investment | null = null;
      vault.updateInvestments(prev => prev.map(inv => {
        if (inv.id === id) {
          const totalCost = inv.quantity * inv.purchasePrice + quantity * price;
          const newQty = inv.quantity + quantity;
          const newAvg = newQty > 0 ? totalCost / newQty : 0;
          updated = {
            ...inv, quantity: newQty, purchasePrice: newAvg,
            currentValue: newQty * inv.currentPrice,
            profitLoss: (newQty * inv.currentPrice) - totalCost,
          };
          return updated;
        }
        return inv;
      }));
      return updated ?? {} as Investment;
    },

    sellUnits: async (id: number, quantity: number, _price: number) => {
      let result: Investment | null = null;
      vault.updateInvestments(prev => {
        return prev.reduce<Investment[]>((acc, inv) => {
          if (inv.id === id) {
            const remaining = inv.quantity - quantity;
            if (remaining <= 0) return acc;
            result = {
              ...inv, quantity: remaining,
              currentValue: remaining * inv.currentPrice,
              profitLoss: (remaining * inv.currentPrice) - (remaining * inv.purchasePrice),
            };
            acc.push(result);
          } else {
            acc.push(inv);
          }
          return acc;
        }, []);
      });
      return result;
    },

    bulkDelete: async (ids: number[]) => {
      const set = new Set(ids);
      vault.updateInvestments(prev => prev.filter(i => !set.has(i.id!)));
      return { deleted: ids.length };
    },
  };
}

export function useLoanApi(): typeof loanApi {
  const vault = useLocalVault();
  if (!vault.isLocalMode) return loanApi;

  return {
    getAll: async () => vault.data.loans,

    getById: async (id: number) => {
      const l = vault.data.loans.find(x => x.id === id);
      if (!l) throw new Error('Loan not found');
      return l;
    },

    create: async (data: Omit<Loan, 'id'>) => {
      const newLoan: Loan = { ...data, id: genId() };
      vault.updateLoans(prev => [...prev, newLoan]);
      return newLoan;
    },

    update: async (id: number, data: Partial<Loan>) => {
      let updated: Loan | null = null;
      vault.updateLoans(prev => prev.map(l => {
        if (l.id === id) { updated = { ...l, ...data, id }; return updated!; }
        return l;
      }));
      return updated ?? { ...data, id } as Loan;
    },

    delete: async (id: number) => {
      vault.updateLoans(prev => prev.filter(l => l.id !== id));
    },

    getSummary: async (): Promise<LoanSummary> => {
      const totalBalance = vault.data.loans.reduce((s, l) => s + l.currentBalance, 0);
      return { totalBalance };
    },

    bulkDelete: async (ids: number[]) => {
      const set = new Set(ids);
      vault.updateLoans(prev => prev.filter(l => !set.has(l.id!)));
      return { deleted: ids.length };
    },
  };
}

export function useSipApi(): typeof sipApi {
  const vault = useLocalVault();
  if (!vault.isLocalMode) return sipApi;

  return {
    getAll: async () => vault.data.sips,

    getById: async (id: number) => {
      const s = vault.data.sips.find(x => x.id === id);
      if (!s) throw new Error('SIP not found');
      return s;
    },

    create: async (data: Omit<Sip, 'id'>) => {
      const newSip: Sip = { ...data, id: genId(), totalInvested: 0, currentValue: 0, completedInstallments: 0 };
      vault.updateSips(prev => [...prev, newSip]);
      return newSip;
    },

    update: async (id: number, data: Partial<Sip>) => {
      let updated: Sip | null = null;
      vault.updateSips(prev => prev.map(s => {
        if (s.id === id) { updated = { ...s, ...data, id }; return updated!; }
        return s;
      }));
      return updated ?? { ...data, id } as Sip;
    },

    delete: async (id: number) => {
      vault.updateSips(prev => prev.filter(s => s.id !== id));
    },

    getSummary: async (): Promise<SipSummary> => {
      const totalInvestment = vault.data.sips.reduce((s, sip) => s + (sip.totalInvested || 0), 0);
      const totalCurrentValue = vault.data.sips.reduce((s, sip) => s + (sip.currentValue || 0), 0);
      return { totalInvestment, totalCurrentValue, totalProfitLoss: totalCurrentValue - totalInvestment };
    },

    pay: async (id: number) => {
      let updated: Sip | null = null;
      vault.updateSips(prev => prev.map(s => {
        if (s.id === id) {
          const invested = (s.totalInvested || 0) + s.monthlyAmount;
          const nav = s.currentNav || 1;
          const newUnits = (s.totalUnits || 0) + (s.monthlyAmount / nav);
          const nextDate = new Date(s.startDate);
          nextDate.setMonth(nextDate.getMonth() + 1);
          updated = {
            ...s,
            totalInvested: invested,
            totalUnits: newUnits,
            currentValue: newUnits * nav,
            completedInstallments: (s.completedInstallments || 0) + 1,
            startDate: nextDate.toISOString().split('T')[0],
            lastInvestmentDate: new Date().toISOString().split('T')[0],
          };
          return updated;
        }
        return s;
      }));
      return updated ?? {} as Sip;
    },

    refreshNavs: async () => {
      throw new Error('NAV refresh is not available in local vault mode');
    },

    bulkDelete: async (ids: number[]) => {
      const set = new Set(ids);
      vault.updateSips(prev => prev.filter(s => !set.has(s.id!)));
      return { deleted: ids.length };
    },
  };
}

export function useSummaryApi(): typeof summaryApi {
  const vault = useLocalVault();
  if (!vault.isLocalMode) return summaryApi;

  return {
    getComprehensiveSummary: async (startDate?: string, endDate?: string): Promise<ComprehensiveFinanceSummary> => {
      const expenses = vault.data.expenses.filter(e =>
        (!startDate || e.date >= startDate) && (!endDate || e.date <= endDate)
      );
      const totalExpenses = expenses.reduce((s, e) => s + e.amount, 0);
      const byCategory: Record<string, number> = {};
      expenses.forEach(e => { byCategory[e.category] = (byCategory[e.category] || 0) + e.amount; });

      const totalInvValue = vault.data.investments.reduce((s, inv) =>
        s + (inv.currentValue ?? inv.quantity * inv.currentPrice), 0);
      const totalInvPL = vault.data.investments.reduce((s, inv) => {
        const val = inv.currentValue ?? inv.quantity * inv.currentPrice;
        const cost = inv.quantity * inv.purchasePrice;
        return s + (val - cost);
      }, 0);

      const totalLoanBalance = vault.data.loans.reduce((s, l) => s + l.currentBalance, 0);

      const sipInvestment = vault.data.sips.reduce((s, sip) => s + (sip.totalInvested || 0), 0);
      const sipValue = vault.data.sips.reduce((s, sip) => s + (sip.currentValue || 0), 0);

      const totalAssets = totalInvValue + sipValue;
      const totalLiabilities = totalLoanBalance;
      const netWorth = totalAssets - totalLiabilities;

      const months = new Set(vault.data.expenses.map(e => e.date.slice(0, 7)));
      const totalAllExpenses = vault.data.expenses.reduce((s, e) => s + e.amount, 0);
      const averageMonthlyExpense = months.size > 0 ? totalAllExpenses / months.size : 0;

      return {
        expenseSummary: { totalExpenses, expensesByCategory: byCategory },
        investmentSummary: { totalValue: totalInvValue, totalProfitLoss: totalInvPL },
        loanSummary: { totalBalance: totalLoanBalance },
        sipSummary: { totalInvestment: sipInvestment, totalCurrentValue: sipValue, totalProfitLoss: sipValue - sipInvestment },
        totalAssets,
        totalLiabilities,
        netWorth,
        averageMonthlyExpense,
      };
    },
  };
}
