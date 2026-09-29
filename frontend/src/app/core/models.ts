export type TransactionalType = 'CASH_ENTRY' | 'EXPENSES';

export type CategoryName =
  | 'WAGE' | 'EXTRA_INCOME' | 'OTHER_INCOME'
  | 'FOOD' | 'LEISURE' | 'HOUSING' | 'HEALTH' | 'TRANSPORT' | 'INVESTMENTS' | 'BILLS' | 'OTHER_EXPENSE';

export type TransactionSource = 'MANUAL' | 'BANK';

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
  /** Quando o gasto ocorreu: a data que a tela mostra. */
  occurredAt: string;
  source: TransactionSource;
  /** Quando o registro entrou no sistema. Nao e a data do gasto. */
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
  OTHER_INCOME: 'Outras receitas',
  FOOD: 'Alimentação',
  LEISURE: 'Lazer',
  HOUSING: 'Moradia',
  HEALTH: 'Saúde',
  TRANSPORT: 'Transporte',
  INVESTMENTS: 'Investimentos',
  BILLS: 'Contas',
  OTHER_EXPENSE: 'Outras despesas',
};

/** O backend recusa categoria que nao pertence ao tipo, entao a UI espelha a regra. */
export const CATEGORIES_BY_TYPE: Record<TransactionalType, CategoryName[]> = {
  CASH_ENTRY: ['WAGE', 'EXTRA_INCOME', 'OTHER_INCOME'],
  EXPENSES: ['FOOD', 'LEISURE', 'HOUSING', 'HEALTH', 'TRANSPORT', 'INVESTMENTS', 'BILLS', 'OTHER_EXPENSE'],
};

export type BankConnectionStatus = 'ACTIVE' | 'ERROR';

export interface BankConnection {
  id: string;
  provider: string;
  institutionName: string | null;
  status: BankConnectionStatus;
  lastSyncedAt: string | null;
  createdAt: string;
}

export interface ConnectToken {
  token: string;
  /** Nome do provedor ativo: define qual widget abrir ("mock" = modo demonstracao). */
  provider: string;
}

export interface BankSyncResult {
  imported: number;
  skipped: number;
}

/** Provedor de demonstracao do backend: nao tem widget, conecta direto. */
export const MOCK_PROVIDER = 'mock';

/** Pluggy: a conexao e feita pelo widget deles. */
export const PLUGGY_PROVIDER = 'pluggy';
