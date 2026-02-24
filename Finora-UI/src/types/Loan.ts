export interface Loan {
  id?: number;
  name: string;
  principalAmount: number;
  interestRate: number;
  interestType: string; // 'SIMPLE', 'COMPOUND'
  compoundingFrequency?: string; // 'MONTHLY', 'QUARTERLY', 'YEARLY'
  startDate: string; // ISO date string
  tenureMonths: number;
  emiAmount?: number;
  currentBalance: number;
  lastUpdated?: string; // ISO date string
  
  // Computed properties
  endDate?: string;
  remainingMonths?: number;
  totalRepayment?: number;
  totalInterest?: number;
}

export interface LoanSummary {
  totalBalance: number;
}