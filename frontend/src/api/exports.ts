import { api, downloadBlob } from "./client";

export const exportsApi = {
  transactionsPdf: async (month: string) => {
    const res = await api.get("/exports/transactions.pdf", {
      params: { month },
      responseType: "blob",
    });
    downloadBlob(res.data as Blob, `transactions-${month}.pdf`);
  },
  transactionsXlsx: async (month: string) => {
    const res = await api.get("/exports/transactions.xlsx", {
      params: { month },
      responseType: "blob",
    });
    downloadBlob(res.data as Blob, `transactions-${month}.xlsx`);
  },
};
