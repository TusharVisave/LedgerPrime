import { api } from "./client";
import type {
  ImportResult,
  Page,
  ParsedTransaction,
  ReceiptScanResult,
  TransactionDto,
  TransactionFilters,
  TransactionMutationResponse,
} from "./types";

// Multipart uploads must NOT use the axios default
// "Content-Type: application/json" — pass FormData and let the browser
// set the multipart boundary on this request only.
const MULTIPART = { headers: { "Content-Type": undefined as unknown as string } };

export interface TransactionInput {
  type: "income" | "expense";
  amount: string; // decimal string, e.g. "1250.50"
  category: string;
  tags?: string[];
  note?: string;
  date: string; // YYYY-MM-DD
}

/** Receipt image bytes for a transaction (fetched with credentials). */
export function receiptBlob(id: number) {
  return api
    .get<Blob>(`/transactions/${id}/receipt`, { responseType: "blob" })
    .then((r) => r.data);
}

export const transactionsApi = {
  list: (filters: TransactionFilters = {}) => {
    const params: Record<string, string | number> = {};
    for (const [k, v] of Object.entries(filters)) {
      if (v !== undefined && v !== null && v !== "") params[k] = v;
    }
    return api.get<Page<TransactionDto>>("/transactions", { params }).then((r) => r.data);
  },
  create: (input: TransactionInput) =>
    api.post<TransactionMutationResponse>("/transactions", input).then((r) => r.data),
  update: (id: number, input: TransactionInput) =>
    api.put<TransactionMutationResponse>(`/transactions/${id}`, input).then((r) => r.data),
  remove: (id: number) =>
    api.delete<{ message: string; budgetStatus: unknown }>(`/transactions/${id}`).then((r) => r.data),
  /** Natural-language quick-add: parse free text into a transaction draft. */
  parse: (text: string) =>
    api.post<ParsedTransaction>("/transactions/parse", { text }).then((r) => r.data),
  /** Scan a receipt image; returns merchant/amount/date/category when OCR works. */
  scanReceipt: (image: File) => {
    const form = new FormData();
    form.append("image", image);
    return api.post<ReceiptScanResult>("/receipts/scan", form, MULTIPART).then((r) => r.data);
  },
  /** Attach a receipt image to an existing transaction. */
  attachReceipt: (id: number, image: File) => {
    const form = new FormData();
    form.append("image", image);
    return api.post(`/transactions/${id}/receipt`, form, MULTIPART).then((r) => r.data);
  },
  /** Import transactions from a CSV file (date,description,amount,type columns). */
  importCsv: (file: File) => {
    const form = new FormData();
    form.append("file", file);
    return api.post<ImportResult>("/transactions/import", form, MULTIPART).then((r) => r.data);
  },
};
