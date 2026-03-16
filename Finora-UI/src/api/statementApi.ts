import apiClient from './apiClient';

export interface StatementPreviewResponse {
  holdings: ParsedHolding[];
  mfHoldings: ParsedMFHolding[];
  warnings: string[];
}

export interface ParsedHolding {
  isin: string;
  name: string;
  symbol: string | null;
  quantity: number;
  avgCost: number | null;
  ltp: number | null;
  importSource: string;
  detectedType: string;
  status: ImportStatus;
}

export interface ParsedMFHolding {
  isin: string;
  schemeCode: string | null;
  schemeName: string;
  units: number;
  avgCost: number | null;
  nav: number | null;
  status: ImportStatus;
}

export enum ImportStatus {
  NEW = 'NEW',
  UPDATE = 'UPDATE',
  SKIP = 'SKIP',
  SKIP_MANUAL = 'SKIP_MANUAL',
}

export interface StatementConfirmRequest {
  selectedIsins: string[];
  statementType: string;
  holdings: ParsedHolding[];
  mfHoldings: ParsedMFHolding[];
}

export interface StatementImportResult {
  imported: number;
  updated: number;
  skipped: number;
  skippedReasons: Record<string, string>;
  warnings: string[];
}

const statementApi = {
  preview: async (
    file: File,
    statementType: string,
    password?: string
  ): Promise<StatementPreviewResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('statementType', statementType);
    if (password) formData.append('password', password);

    const response = await apiClient.post<{ data: StatementPreviewResponse }>(
      '/statements/preview',
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
    return response.data.data;
  },

  confirm: async (request: StatementConfirmRequest): Promise<StatementImportResult> => {
    const response = await apiClient.post<{ data: StatementImportResult }>(
      '/statements/confirm',
      request,
      { timeout: 120000 }
    );
    return response.data.data;
  },
};

export default statementApi;
