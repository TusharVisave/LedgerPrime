import { api } from "./client";
import type { SubscriptionsDto } from "./types";

export const subscriptionsApi = {
  list: () => api.get<SubscriptionsDto>("/subscriptions").then((r) => r.data),
};
