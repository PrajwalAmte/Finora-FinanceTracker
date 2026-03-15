import * as XLSX from 'xlsx';
import { Expense } from '../types/Expense';
import { Investment } from '../types/Investment';
import { Loan } from '../types/Loan';
import { Sip } from '../types/Sip';
import { formatCurrency, formatDate, formatPercentage } from './formatters';

export const generateExpenseReport = (
  expenses: Expense[],
  title: string,
  startDate?: string,
  endDate?: string,
  totalAmount?: number
): void => {
  const wb = XLSX.utils.book_new();
  
  const calculatedTotal = totalAmount ?? expenses.reduce((sum, expense) => sum + expense.amount, 0);
  
  const data = expenses.map(expense => ({
    Date: formatDate(expense.date, 'MM/dd/yyyy'),
    Description: expense.description,
    Category: expense.category,
    'Payment Method': expense.paymentMethod,
    Amount: formatCurrency(expense.amount)
  }));

  data.push({
    Date: 'TOTAL',
    Description: '',
    Category: '',
    'Payment Method': '',
    Amount: formatCurrency(calculatedTotal)
  });

  const ws = XLSX.utils.json_to_sheet(data);

  const colWidths = [
    { wch: 12 },
    { wch: 30 },
    { wch: 15 },
    { wch: 15 },
    { wch: 15 }
  ];
  ws['!cols'] = colWidths;

  const totalRowIndex = data.length;
  const totalRowCells = ['A', 'B', 'C', 'D', 'E'].map(col => `${col}${totalRowIndex}`);
  totalRowCells.forEach(cell => {
    if (!ws[cell]) ws[cell] = {};
    ws[cell].s = { font: { bold: true } };
  });

  XLSX.utils.book_append_sheet(wb, ws, 'Expenses');

  const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '');
  const periodStr = startDate && endDate ? `_${startDate.replace(/-/g, '')}-${endDate.replace(/-/g, '')}` : '';
  XLSX.writeFile(wb, `expenses-report-${dateStr}${periodStr}.xlsx`);
};

export const generateInvestmentReport = (
  investments: Investment[],
  title: string,
  totalValue?: number,
  totalProfitLoss?: number
): void => {
  const wb = XLSX.utils.book_new();
  
  const calculatedTotalValue = totalValue ?? investments.reduce((sum, inv) => {
    const currentValue = inv.currentValue ?? (inv.quantity * inv.currentPrice);
    return sum + currentValue;
  }, 0);
  
  const calculatedTotalProfitLoss = totalProfitLoss ?? investments.reduce((sum, inv) => {
    const currentValue = inv.currentValue ?? (inv.quantity * inv.currentPrice);
    const purchaseValue = inv.quantity * inv.purchasePrice;
    const profitLoss = inv.profitLoss ?? (currentValue - purchaseValue);
    return sum + profitLoss;
  }, 0);

  const calculatedTotalReturnPercentage = investments.length > 0 ? 
    (calculatedTotalProfitLoss / (calculatedTotalValue - calculatedTotalProfitLoss)) * 100 : 0;
  
  const data = investments.map(investment => {
    const currentValue = investment.currentValue ?? (investment.quantity * investment.currentPrice);
    const purchaseValue = investment.quantity * investment.purchasePrice;
    const profitLoss = investment.profitLoss ?? (currentValue - purchaseValue);
    const returnPercentage = investment.returnPercentage ?? 
      (purchaseValue > 0 ? (profitLoss / purchaseValue) * 100 : 0);

    return {
      Name: investment.name,
      Symbol: investment.symbol,
      Type: investment.type,
      Quantity: investment.quantity,
      'Purchase Price': formatCurrency(investment.purchasePrice),
      'Current Price': formatCurrency(investment.currentPrice),
      'Purchase Date': formatDate(investment.purchaseDate),
      'Current Value': formatCurrency(currentValue),
      'Profit/Loss': formatCurrency(profitLoss),
      'Return %': formatPercentage(returnPercentage)
    };
  });

  data.push({
    Name: 'TOTAL',
    Symbol: '',
    Type: '',
    Quantity: 0,
    'Purchase Price': '',
    'Current Price': '',
    'Purchase Date': '',
    'Current Value': formatCurrency(calculatedTotalValue),
    'Profit/Loss': formatCurrency(calculatedTotalProfitLoss),
    'Return %': formatPercentage(calculatedTotalReturnPercentage)
  });

  const ws = XLSX.utils.json_to_sheet(data);
  
  const colWidths = [
    { wch: 20 },
    { wch: 10 },
    { wch: 15 },
    { wch: 10 },
    { wch: 15 },
    { wch: 15 },
    { wch: 12 },
    { wch: 15 },
    { wch: 15 },
    { wch: 12 }
  ];
  ws['!cols'] = colWidths;

  const totalRowIndex = data.length;
  const totalRowCells = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'].map(col => `${col}${totalRowIndex}`);
  totalRowCells.forEach(cell => {
    if (!ws[cell]) ws[cell] = {};
    ws[cell].s = { font: { bold: true } };
  });

  XLSX.utils.book_append_sheet(wb, ws, 'Investments');

  const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '');
  XLSX.writeFile(wb, `investments-report-${dateStr}.xlsx`);
};

export const generateLoanReport = (
  loans: Loan[],
  title: string,
  totalBalance?: number
): void => {
  const wb = XLSX.utils.book_new();
  
  const calculatedTotalBalance = totalBalance ?? loans.reduce((sum, loan) => sum + loan.currentBalance, 0);
  const calculatedTotalPrincipal = loans.reduce((sum, loan) => sum + loan.principalAmount, 0);
  const calculatedTotalEMI = loans.reduce((sum, loan) => sum + (loan.emiAmount || 0), 0);
  const calculatedTotalInterest = loans.reduce((sum, loan) => sum + (loan.totalInterest || 0), 0);
  
  const data = loans.map(loan => {
    const remainingMonths = loan.remainingMonths ?? 
      (loan.emiAmount && loan.emiAmount > 0 ? Math.ceil(loan.currentBalance / loan.emiAmount) : 0);

    return {
      Name: loan.name,
      Principal: formatCurrency(loan.principalAmount),
      'Interest Rate': formatPercentage(loan.interestRate),
      'Interest Type': loan.interestType,
      'Start Date': formatDate(loan.startDate),
      'Tenure (Months)': loan.tenureMonths,
      EMI: formatCurrency(loan.emiAmount || 0),
      'Current Balance': formatCurrency(loan.currentBalance),
      'Remaining Months': remainingMonths,
      'Total Interest': formatCurrency(loan.totalInterest || 0)
    };
  });

  data.push({
    Name: 'TOTAL',
    Principal: formatCurrency(calculatedTotalPrincipal),
    'Interest Rate': '',
    'Interest Type': '',
    'Start Date': '',
    'Tenure (Months)': 0,
    EMI: formatCurrency(calculatedTotalEMI),
    'Current Balance': formatCurrency(calculatedTotalBalance),
    'Remaining Months': 0,
    'Total Interest': formatCurrency(calculatedTotalInterest)
  });

  const ws = XLSX.utils.json_to_sheet(data);
  
  const colWidths = [
    { wch: 20 },
    { wch: 15 },
    { wch: 12 },
    { wch: 15 },
    { wch: 12 },
    { wch: 15 },
    { wch: 15 },
    { wch: 15 },
    { wch: 15 },
    { wch: 15 }
  ];
  ws['!cols'] = colWidths;

  const totalRowIndex = data.length;
  const totalRowCells = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'].map(col => `${col}${totalRowIndex}`);
  totalRowCells.forEach(cell => {
    if (!ws[cell]) ws[cell] = {};
    ws[cell].s = { font: { bold: true } };
  });

  XLSX.utils.book_append_sheet(wb, ws, 'Loans');

  const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '');
  XLSX.writeFile(wb, `loans-report-${dateStr}.xlsx`);
};

export const generateSipReport = (
  sips: Sip[],
  title: string,
  totalInvestment?: number,
  totalCurrentValue?: number,
  totalProfitLoss?: number
): void => {
  const wb = XLSX.utils.book_new();
  
  const calculatedTotalInvestment = totalInvestment ?? sips.reduce((sum, sip) => sum + (sip.totalInvested || 0), 0);
  const calculatedTotalCurrentValue = totalCurrentValue ?? sips.reduce((sum, sip) => sum + (sip.currentValue || 0), 0);
  const calculatedTotalProfitLoss = totalProfitLoss ?? sips.reduce((sum, sip) => sum + (sip.profitLoss || 0), 0);
  const calculatedTotalReturnPercentage = calculatedTotalInvestment > 0 ? 
    (calculatedTotalProfitLoss / calculatedTotalInvestment) * 100 : 0;
  
  const data = sips.map(sip => {
    const totalInvested = sip.totalInvested ?? (sip.monthlyAmount * sip.durationMonths);
    const currentValue = sip.currentValue ?? (sip.totalUnits * sip.currentNav);
    const profitLoss = sip.profitLoss ?? (currentValue - totalInvested);
    const returnPercentage = totalInvested > 0 ? (profitLoss / totalInvested) * 100 : 0;

    return {
      Name: sip.name,
      'Scheme Code': sip.schemeCode,
      'Monthly Amount': formatCurrency(sip.monthlyAmount),
      'Start Date': formatDate(sip.startDate),
      'Duration (Months)': sip.durationMonths,
      'Current NAV': formatCurrency(sip.currentNav),
      'Total Units': sip.totalUnits.toFixed(3),
      'Total Invested': formatCurrency(totalInvested),
      'Current Value': formatCurrency(currentValue),
      'Profit/Loss': formatCurrency(profitLoss),
      'Return %': formatPercentage(returnPercentage)
    };
  });

  data.push({
    Name: 'TOTAL',
    'Scheme Code': '',
    'Monthly Amount': '',
    'Start Date': '',
    'Duration (Months)': 0,
    'Current NAV': '',
    'Total Units': '',
    'Total Invested': formatCurrency(calculatedTotalInvestment),
    'Current Value': formatCurrency(calculatedTotalCurrentValue),
    'Profit/Loss': formatCurrency(calculatedTotalProfitLoss),
    'Return %': formatPercentage(calculatedTotalReturnPercentage)
  });

  const ws = XLSX.utils.json_to_sheet(data);
  
  const colWidths = [
    { wch: 20 },
    { wch: 15 },
    { wch: 15 },
    { wch: 12 },
    { wch: 15 },
    { wch: 15 },
    { wch: 12 },
    { wch: 15 },
    { wch: 15 },
    { wch: 15 },
    { wch: 12 }
  ];
  ws['!cols'] = colWidths;

  const totalRowIndex = data.length;
  const totalRowCells = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K'].map(col => `${col}${totalRowIndex}`);
  totalRowCells.forEach(cell => {
    if (!ws[cell]) ws[cell] = {};
    ws[cell].s = { font: { bold: true } };
  });

  XLSX.utils.book_append_sheet(wb, ws, 'SIPs');

  const dateStr = new Date().toISOString().slice(0, 10).replace(/-/g, '');
  XLSX.writeFile(wb, `sips-report-${dateStr}.xlsx`);
};