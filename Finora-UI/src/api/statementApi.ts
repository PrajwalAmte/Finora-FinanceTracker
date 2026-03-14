import apiClient from './apiClient';

export interface StatementPreviewResponse {
  equities: ParsedHolding[];
  mfs: ParsedMFHolding[];
  warnings: string[];
}

export interface ParsedHolding {
  isin: string;
  name: string;
  quantity: number;
  averageCost: number;
  currentPrice: number | null;
  importStatus: ImportStatus;
  investmentType: string;
}

export interface ParsedMFHolding {
  schemeCode: string;
  schemeName: string;
  units: number;
  costPerUnit: number;
  currentNav: number | null;
  importStatus: ImportStatus;
  status: string;
}

export enum ImportStatus {
  NEW = 'NEW',
  UPDATE = 'UPDATE',
  SKIP = 'SKIP',
}

export interface StatementConfirmRequest {
  statementType: string;
  equities: string[]; // ISINs to import
  mfs: string[]; // scheme codes to import
}

export interface StatementImportResult {
  equitiesImported: number;
  equitiesUpdated: number;
  equitiesSkipped: number;
  mfsImported: number;
  mfsUpdated: number;
  mfsSkipped: number;
  skippedReasons: Record<string, string>;
}

const statementApi = {
  preview: async (
    file: File,
    statementType: 'CAS' | 'CAMS' | 'ZERODHA_EXCEL',
    password?: string
  ): Promise<StatementPreviewResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('statementType', statementType);
    if (password) formData.append('password', password);

    const response = await apiClient.post<StatementPreviewResponse>(
      '/api/statement/preview',
      formData,
      { headers: { 'Content-Type': 'multipart/form-data' } }
    );
    return response.data;
  },

  confirm: async (request: StatementConfirmRequest): Promise<StatementImportResult> => {
    const response = await apiClient.post<StatementImportResult>(
      '/api/statement/confirm',
      request
    );
    return response.data;
  },
};

export default statementApi;
