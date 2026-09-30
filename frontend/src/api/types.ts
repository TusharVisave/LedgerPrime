// ── Shared API types (mirror the Spring Boot contract exactly) ──

export interface UserDto {
  id: number;
  name: string;
  email: string;
}

export type TransactionType = "income" | "expense";

export interface TransactionDto {
  id: number;
  type: TransactionType;
  amount: string; // decimal string, e.g. "1250.50"
  amountPaise: number;
  category: string;
  tags: string[];
  note: string;
  date: string; // YYYY-MM-DD
  recurringGenerated: boolean;
  createdAt: string;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface BudgetStatus {
  month: string; // YYYY-MM
  budget: string;
  spent: string;
  remaining: string;
  exceeded: boolean;
  nearLimit: boolean;
}

export interface TransactionMutationResponse {
  transaction: TransactionDto;
  budgetStatus: BudgetStatus;
}

export interface CategoryDto {
  id: number;
  name: string;
  type: TransactionType;
  color: string;
  icon: string;
  isDefault: boolean;
}

export interface CategorySpending {
  name: string;
  value: string; // decimal string
  color: string;
}

export interface MonthlyTrendPoint {
  month: string; // YYYY-MM
  income: string;
  expense: string;
}

export interface DailyTrendPoint {
  day: number;
  amount: string;
}

export interface DashboardDto {
  summary: {
    month: string;
    income: string;
    expense: string;
    balance: string;
  };
  budget: BudgetStatus;
  categoryBudgets: CategoryBudgetStatus[];
  categorySpending: CategorySpending[];
  monthlyTrend: MonthlyTrendPoint[];
  dailyTrend: DailyTrendPoint[];
  recentTransactions: TransactionDto[];
}

export interface ReminderDto {
  id: number;
  title: string;
  amount: string; // decimal string
  category: string;
  dueDate: string; // YYYY-MM-DD
  isRecurring: boolean;
  recurrence: string; // e.g. "monthly"
  emailNotify: boolean;
  lastTriggeredAt: string | null;
}

export interface InsightDto {
  type: string;
  severity: "info" | "warning" | "success";
  title: string;
  message: string;
}

export interface InsightsDto {
  month: string;
  insights: InsightDto[];
  anomalies: AnomalyDto[];
}

export interface AnomalyDto {
  category: string;
  recentSpend: string;
  baselineAverage: string;
  multiplier: string; // e.g. "3.2x"
}

export interface AiReportDto {
  month: string;
  report: string;
}

export interface CategoryBudgetStatus {
  category: string;
  budget: string;
  spent: string;
  remaining: string;
  exceeded: boolean;
  nearLimit: boolean;
  rollover: boolean;
}

export interface GoalDto {
  id: number;
  name: string;
  targetAmount: string; // decimal string
  targetPaise: number;
  deadline: string | null; // YYYY-MM-DD
  allocated: string;
  allocatedPaise: number;
  remaining: string;
  progressPercent: number;
}

export interface GoalInput {
  name: string;
  targetAmount: string; // decimal string "50000.00"
  deadline?: string; // YYYY-MM-DD, optional
}

export interface GoalAllocateInput {
  amount: string; // decimal string
  note?: string;
  date?: string; // YYYY-MM-DD
}

export interface SubscriptionDto {
  id: number;
  title: string;
  amount: string; // decimal string
  category: string;
  dueDate: string; // YYYY-MM-DD
  recurrence: string; // e.g. "monthly"
  monthlyCost: string;
}

export interface SubscriptionsDto {
  subscriptions: SubscriptionDto[];
  totalMonthlyCost: string;
  totalMonthlyCostPaise: number;
  count: number;
}

export interface ParsedTransaction {
  type: "income" | "expense";
  amount: string | null; // decimal string or null when not detected
  category: string;
  date: string; // YYYY-MM-DD
  note: string;
}

export interface ReceiptScanResult {
  merchant: string | null;
  amount: string | null;
  date: string | null; // YYYY-MM-DD
  category: string | null;
  rawText: string | null;
  ocrAvailable: boolean;
  storedImage: string | null;
}

export interface ImportRowError {
  row: number;
  message: string;
}

export interface ImportResult {
  importedCount: number;
  failedCount: number;
  errors: ImportRowError[];
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}

export interface TransactionFilters {
  type?: "" | TransactionType;
  category?: string;
  keyword?: string;
  minAmount?: string;
  maxAmount?: string;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
  sort?: string;
}
