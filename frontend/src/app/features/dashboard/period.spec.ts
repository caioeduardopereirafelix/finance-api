import { compare, daysIn, formatRange, isValidRange, previousRange, rangeFor } from './period';

describe('period', () => {

  const today = new Date(2026, 8, 29);   // 29/09/2026

  it('este mês vai do dia 1 até hoje', () => {
    expect(rangeFor('THIS_MONTH', today)).toEqual({ start: '2026-09-01', end: '2026-09-29' });
  });

  it('mês passado é o mês anterior inteiro', () => {
    expect(rangeFor('LAST_MONTH', today)).toEqual({ start: '2026-08-01', end: '2026-08-31' });
  });

  it('mês passado em janeiro volta para dezembro do ano anterior', () => {
    expect(rangeFor('LAST_MONTH', new Date(2026, 0, 15))).toEqual({ start: '2025-12-01', end: '2025-12-31' });
  });

  it('últimos 30 dias incluem hoje (30 dias no total)', () => {
    const range = rangeFor('LAST_30', today);
    expect(range).toEqual({ start: '2026-08-31', end: '2026-09-29' });
    expect(daysIn(range)).toBe(30);
  });

  it('últimos 90 dias e este ano', () => {
    expect(daysIn(rangeFor('LAST_90', today))).toBe(90);
    expect(rangeFor('THIS_YEAR', today)).toEqual({ start: '2026-01-01', end: '2026-09-29' });
  });

  it('o período anterior tem o mesmo tamanho e termina na véspera', () => {
    const range = { start: '2026-09-01', end: '2026-09-29' };
    const previous = previousRange(range);

    expect(previous).toEqual({ start: '2026-08-03', end: '2026-08-31' });
    expect(daysIn(previous)).toBe(daysIn(range));
  });

  it('o período anterior atravessa a virada de ano', () => {
    expect(previousRange({ start: '2026-01-01', end: '2026-01-10' })).toEqual({ start: '2025-12-22', end: '2025-12-31' });
  });

  it('formata as datas para o leitor', () => {
    expect(formatRange({ start: '2026-09-01', end: '2026-09-29' })).toBe('01/09/2026 a 29/09/2026');
  });

  it('valida o intervalo', () => {
    expect(isValidRange({ start: '2026-09-01', end: '2026-09-01' })).toBe(true);
    expect(isValidRange({ start: '2026-09-02', end: '2026-09-01' })).toBe(false);
    expect(isValidRange({ start: '', end: '2026-09-01' })).toBe(false);
  });

  describe('compare', () => {
    it('aumento e queda em porcentagem', () => {
      expect(compare(150, 100)).toEqual({ direction: 'up', percent: 50, absolute: 50 });
      expect(compare(50, 100)).toEqual({ direction: 'down', percent: -50, absolute: -50 });
    });

    it('sem base de comparação quando o período anterior é zero', () => {
      expect(compare(100, 0).direction).toBe('none');
      expect(compare(100, 0).percent).toBeNull();
    });

    it('sem variação quando iguais, inclusive os dois zerados', () => {
      expect(compare(100, 100).direction).toBe('flat');
      expect(compare(0, 0).direction).toBe('flat');
    });

    it('ignora diferença de centavos de arredondamento', () => {
      expect(compare(100.001, 100).direction).toBe('flat');
    });
  });
});
