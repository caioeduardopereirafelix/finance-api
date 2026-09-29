import { CategoryTotal } from '../../core/models';
import { buildBars, formatShare } from './category-bars';

const rows: CategoryTotal[] = [
  { category: 'TRANSPORT', type: 'EXPENSES', total: 100, count: 2 },
  { category: 'FOOD', type: 'EXPENSES', total: 300, count: 5 },
  { category: 'HEALTH', type: 'EXPENSES', total: 100, count: 1 },
  { category: 'WAGE', type: 'CASH_ENTRY', total: 5000, count: 1 },
];

describe('buildBars', () => {

  it('mostra só o tipo pedido, do maior para o menor', () => {
    expect(buildBars(rows, 'EXPENSES').map(b => b.category)).toEqual(['FOOD', 'TRANSPORT', 'HEALTH']);
    expect(buildBars(rows, 'CASH_ENTRY').map(b => b.category)).toEqual(['WAGE']);
  });

  it('a maior barra vale 1 e as outras são proporcionais a ela', () => {
    const bars = buildBars(rows, 'EXPENSES');
    expect(bars[0].scale).toBe(1);
    expect(bars[1].scale).toBeCloseTo(1 / 3);
  });

  it('a participação de cada uma soma 100%', () => {
    const bars = buildBars(rows, 'EXPENSES');
    expect(bars.reduce((sum, b) => sum + b.share, 0)).toBeCloseTo(1);
    expect(bars[0].share).toBeCloseTo(0.6);
  });

  it('usa o nome da categoria em português', () => {
    expect(buildBars(rows, 'EXPENSES')[0].label).toBe('Alimentação');
  });

  it('ignora total zerado e lista vazia', () => {
    expect(buildBars([{ category: 'FOOD', type: 'EXPENSES', total: 0, count: 0 }], 'EXPENSES')).toEqual([]);
    expect(buildBars([], 'EXPENSES')).toEqual([]);
  });

  it('formata a participação, sem mostrar 0% para algo que existe', () => {
    expect(formatShare(0.354)).toBe('35%');
    expect(formatShare(0.004)).toBe('<1%');
    expect(formatShare(1)).toBe('100%');
  });
});
