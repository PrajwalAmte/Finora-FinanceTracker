export interface Expense {
  id?: number;
  description: string;
  amount: number;
  date: string; // ISO date string
  category: string;
  paymentMethod: string;
}

export interface ExpenseSummary {
  totalExpenses: number;
  expensesByCategory: Record<string, number>;
}