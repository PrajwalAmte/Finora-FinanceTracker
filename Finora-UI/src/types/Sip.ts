export interface Sip {
  id?: number;
  name: string;
  schemeCode: string;
  monthlyAmount: number;
  /** Next installment date (stored as startDate in DB). Advances by +1 month after each payment. */
  startDate: string;
  durationMonths: number;
  currentNav?: number;
  totalUnits?: number;
  lastUpdated?: string;
  lastInvestmentDate?: string;
  isin?: string;
  importSource?: string;

  // Stored on the server — actual money paid in so far
  totalInvested?: number;

  // Server-computed
  currentValue?: number;
  completedInstallments?: number;
  profitLoss?: number;
  /** Set when this SIP is backed by a linked Investment (units/NAV/value come from there). */
  investmentId?: number;
}

export interface SipSummary {
  totalInvestment: number;
  totalCurrentValue: number;
  totalProfitLoss: number;
}