import { CATEGORY_LABEL, CategoryName, CategoryTotal, TransactionalType } from '../../core/models';

export interface Bar {
  category: CategoryName;
  label: string;
  total: number;
  count: number;
  share: number;
  scale: number;
}

export function buildBars(items: CategoryTotal[], type: TransactionalType): Bar[] {
  const rows = items
    .filter((item) => item.type === type && item.total > 0)
    .sort((a, b) => b.total - a.total);

  const sum = rows.reduce((acc, row) => acc + row.total, 0);
  const max = rows.length ? rows[0].total : 0;

  return rows.map((row) => ({
    category: row.category,
    label: CATEGORY_LABEL[row.category],
    total: row.total,
    count: row.count,
    share: sum ? row.total / sum : 0,
    scale: max ? row.total / max : 0,
  }));
}

export function formatShare(share: number): string {
  if (share > 0 && share < 0.01) return '<1%';
  return `${Math.round(share * 100)}%`;
}
