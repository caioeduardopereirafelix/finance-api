export type PeriodPreset = 'THIS_MONTH' | 'LAST_MONTH' | 'LAST_30' | 'LAST_90' | 'THIS_YEAR' | 'CUSTOM';

export interface DateRange {
  start: string;
  end: string;
}

export const PERIOD_PRESETS: { id: PeriodPreset; label: string }[] = [
  { id: 'THIS_MONTH', label: 'Este mês' },
  { id: 'LAST_MONTH', label: 'Mês passado' },
  { id: 'LAST_30', label: 'Últimos 30 dias' },
  { id: 'LAST_90', label: 'Últimos 90 dias' },
  { id: 'THIS_YEAR', label: 'Este ano' },
  { id: 'CUSTOM', label: 'Personalizado' },
];

const pad = (n: number) => String(n).padStart(2, '0');

export function toIso(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function fromIso(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
}

export function addDays(date: Date, days: number): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days);
}

export function rangeFor(preset: Exclude<PeriodPreset, 'CUSTOM'>, today: Date): DateRange {
  const year = today.getFullYear();
  const month = today.getMonth();

  switch (preset) {
    case 'THIS_MONTH': return { start: toIso(new Date(year, month, 1)), end: toIso(today) };
    case 'LAST_MONTH': return { start: toIso(new Date(year, month - 1, 1)), end: toIso(new Date(year, month, 0)) };
    case 'LAST_30': return { start: toIso(addDays(today, -29)), end: toIso(today) };
    case 'LAST_90': return { start: toIso(addDays(today, -89)), end: toIso(today) };
    case 'THIS_YEAR': return { start: toIso(new Date(year, 0, 1)), end: toIso(today) };
  }
}

export function isValidRange(range: DateRange): boolean {
  const iso = /^\d{4}-\d{2}-\d{2}$/;
  return iso.test(range.start) && iso.test(range.end) && range.start <= range.end;
}

export function daysIn(range: DateRange): number {
  return Math.round((fromIso(range.end).getTime() - fromIso(range.start).getTime()) / 86_400_000) + 1;
}

export function previousRange(range: DateRange): DateRange {
  const length = daysIn(range);
  const start = fromIso(range.start);
  return { start: toIso(addDays(start, -length)), end: toIso(addDays(start, -1)) };
}

export function formatDate(iso: string): string {
  const [year, month, day] = iso.split('-');
  return `${day}/${month}/${year}`;
}

export function formatRange(range: DateRange): string {
  return `${formatDate(range.start)} a ${formatDate(range.end)}`;
}

export interface Delta {
  direction: 'up' | 'down' | 'flat' | 'none';
  percent: number | null;
  absolute: number;
}

export function compare(current: number, previous: number): Delta {
  const absolute = current - previous;
  if (Math.abs(absolute) < 0.005) {
    return { direction: 'flat', percent: previous === 0 ? null : 0, absolute: 0 };
  }
  if (previous === 0) {
    return { direction: 'none', percent: null, absolute };
  }
  return {
    direction: absolute > 0 ? 'up' : 'down',
    percent: (absolute / Math.abs(previous)) * 100,
    absolute,
  };
}
