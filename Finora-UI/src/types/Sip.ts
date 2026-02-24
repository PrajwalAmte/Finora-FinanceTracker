export interface Sip {
  id?: number;
  name: string;
  schemeCode: string;
  monthlyAmount: number;
  startDate: string; // ISO date string
  durationMonths: number;
  currentNav: number;
  totalUnits: number;
  lastUpdated?: string; // ISO date string
  lastInvestmentDate?: string; // ISO date string
  
  // Computed properties
  currentValue?: number;
  completedInstallments?: number;
  totalInvested?: number;
  profitLoss?: number;
}

export interface SipSummary {
  totalInvestment: number;
  totalCurrentValue: number;
  totalProfitLoss: number;
}