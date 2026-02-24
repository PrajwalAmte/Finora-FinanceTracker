export interface Investment {
  id?: number;
  name: string;
  symbol: string;
  type: string; // 'STOCK', 'MUTUAL_FUND', etc.
  quantity: number;
  purchasePrice: number;
  currentPrice: number;
  purchaseDate: string; // ISO date string
  lastUpdated?: string; // ISO date string
  
  // These are computed server-side but we'll include them for convenience
  currentValue?: number;
  profitLoss?: number;
  returnPercentage?: number;
}

export interface InvestmentSummary {
  totalValue: number;
  totalProfitLoss: number;
}