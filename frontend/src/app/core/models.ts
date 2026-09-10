export type TransactionalType = 'CASH_ENTRY' | 'EXPENSES';

export type CategoryName =
  | 'WAGE' | 'EXTRA_INCOME'
  | 'FOOD' | 'LEISURE' | 'HOUSING' | 'HEALTH' | 'TRANSPORT' | 'INVESTMENTS' | 'BILLS';

export interface AuthResponse {
  token: string;
  expiresIn: number;
  refreshToken: string;
}

export interface Transaction {
  id: string;
  description: string;
  amount: number;
  category: CategoryName;
  type: TransactionalType;
  createdDate: string;
}

export interface TransactionPayload {
  description: string;
  amount: number;
  type: TransactionalType;
  category: CategoryName;
}

export interface Summary {
  cashEntry: number;
  expenses: number;
  balance: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

export interface FieldError {
  field: string;
  message: string;
}

export interface ApiError {
  status: number;
  error: string;
  fieldsError: FieldError[];
}

export interface TransactionFilters {
  type?: TransactionalType | '';
  category?: CategoryName | '';
  description?: string;
  minAmount?: number | null;
  maxAmount?: number | null;
  startDate?: string;
  endDate?: string;
  page?: number;
  size?: number;
}

/** Rotulos em portugues, usados em telas e em textos lidos por leitor de tela. */
export const TYPE_LABEL: Record<TransactionalType, string> = {
  CASH_ENTRY: 'Entrada',
  EXPENSES: 'Saída',
};

export const CATEGORY_LABEL: Record<CategoryName, string> = {
  WAGE: 'Salário',
  EXTRA_INCOME: 'Renda extra',
  FOOD: 'Alimentação',
  LEISURE: 'Lazer',
  HOUSING: 'Moradia',
  HEALTH: 'Saúde',
  TRANSPORT: 'Transporte',
  INVESTMENTS: 'Investimentos',
  BILLS: 'Contas',
};

/** O backend recusa categoria que nao pertence ao tipo, entao a UI espelha a regra. */
export const CATEGORIES_BY_TYPE: Record<TransactionalType, CategoryName[]> = {
  CASH_ENTRY: ['WAGE', 'EXTRA_INCOME'],
  EXPENSES: ['FOOD', 'LEISURE', 'HOUSING', 'HEALTH', 'TRANSPORT', 'INVESTMENTS', 'BILLS'],
};
