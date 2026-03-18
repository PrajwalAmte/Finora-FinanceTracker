import { Expense } from './Expense';
import { Investment } from './Investment';
import { Loan } from './Loan';
import { Sip } from './Sip';

export interface VaultData {
  expenses: Expense[];
  investments: Investment[];
  loans: Loan[];
  sips: Sip[];
}

export interface VaultFile {
  version: string;
  createdAt: string;
  updatedAt: string;
  data: VaultData;
}
