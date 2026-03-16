import apiClient from './apiClient';

export interface ParsedTransaction {
  date: string;
  narration: string;
  amount: number;
  type: 'DEBIT' | 'CREDIT';
  balance: number | null;
}

export interface ExpensePreviewResponse {
  transactions: ParsedTransaction[];
  warnings: string[];
  bankName: string;
  totalDebits: number;
  totalCredits: number;
}

export interface ExpenseImportEntry {
  date: string;
  description: string;
  amount: number;
  category: string;
  paymentMethod: string;
}

export interface ExpenseImportResult {
  imported: number;
  skipped: number;
}

const expenseImportApi = {
  preview: async (file: File): Promise<ExpensePreviewResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await apiClient.post<{ data: ExpensePreviewResponse }>(
      '/expense-import/preview',
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
    return response.data.data;
  },

  confirm: async (expenses: ExpenseImportEntry[]): Promise<ExpenseImportResult> => {
    const response = await apiClient.post<{ data: ExpenseImportResult }>(
      '/expense-import/confirm',
      { expenses },
      { timeout: 60000 }
    );
    return response.data.data;
  },
};

export default expenseImportApi;
