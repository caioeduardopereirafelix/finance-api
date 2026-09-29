import { Component, computed, inject, OnDestroy, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin, Subscription } from 'rxjs';

import { messageOf } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { CATEGORY_LABEL, CategoryTotal, Summary, Transaction, TYPE_LABEL } from '../../core/models';
import { TransactionService } from '../../core/transaction.service';
import { BrDatePipe } from '../../shared/date.pipe';
import { MoneyPipe } from '../../shared/money.pipe';
import { CategoryChart } from './category-chart';
import {
  compare, DateRange, formatRange, isValidRange, PERIOD_PRESETS, PeriodPreset, previousRange, rangeFor,
} from './period';

interface DashboardData {
  summary: Summary;
  previous: Summary;
  categories: CategoryTotal[];
  recent: Transaction[];
}

interface DeltaView {
  arrow: string;
  text: string;
  tone: 'good' | 'bad' | 'neutral';
  sr: string;
}

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, MoneyPipe, BrDatePipe, CategoryChart],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class DashboardPage implements OnDestroy {

  private readonly transactions = inject(TransactionService);
  private readonly money = new MoneyPipe();
  private inflight?: Subscription;
  readonly auth = inject(AuthService);

  readonly presets = PERIOD_PRESETS;
  readonly preset = signal<PeriodPreset>('THIS_MONTH');
  readonly range = signal<DateRange>(rangeFor('THIS_MONTH', new Date()));
  readonly custom = signal<DateRange>({ start: '', end: '' });
  readonly customError = signal<string | null>(null);

  readonly loading = signal(true);
  readonly refreshing = signal(false);
  readonly error = signal<string | null>(null);
  readonly data = signal<DashboardData | null>(null);

  readonly rangeLabel = computed(() => formatRange(this.range()));
  readonly previousLabel = computed(() => formatRange(previousRange(this.range())));

  readonly typeLabel = TYPE_LABEL;
  readonly categoryLabel = CATEGORY_LABEL;

  readonly incomeDelta = computed(() => {
    const d = this.data();
    return d ? this.percentView(d.summary.cashEntry, d.previous.cashEntry, true) : null;
  });
  readonly expenseDelta = computed(() => {
    const d = this.data();
    return d ? this.percentView(d.summary.expenses, d.previous.expenses, false) : null;
  });
  readonly balanceDelta = computed(() => {
    const d = this.data();
    return d ? this.balanceView(d.summary.balance, d.previous.balance) : null;
  });

  constructor() {
    this.load();
  }

  ngOnDestroy() {
    this.inflight?.unsubscribe();
  }

  load() {
    this.inflight?.unsubscribe();

    if (this.data()) {
      this.refreshing.set(true);
    } else {
      this.loading.set(true);
    }
    this.error.set(null);

    const range = this.range();

    this.inflight = forkJoin({
      summary: this.transactions.summary(range),
      previous: this.transactions.summary(previousRange(range)),
      categories: this.transactions.categoryTotals(range),
      page: this.transactions.list({ startDate: range.start, endDate: range.end, page: 0, size: 5 }),
    }).subscribe({
      next: ({ summary, previous, categories, page }) => {
        this.data.set({ summary, previous, categories, recent: page.content });
        this.loading.set(false);
        this.refreshing.set(false);
      },
      error: (err) => {
        this.error.set(messageOf(err, 'Não foi possível carregar seus dados.'));
        this.loading.set(false);
        this.refreshing.set(false);
      },
    });
  }

  choosePreset(preset: PeriodPreset) {
    this.preset.set(preset);
    this.customError.set(null);

    if (preset === 'CUSTOM') {
      this.custom.set({ ...this.range() });
      return;
    }
    this.range.set(rangeFor(preset, new Date()));
    this.load();
  }

  setCustom(edge: 'start' | 'end', event: Event) {
    const value = (event.target as HTMLInputElement).value;
    const next = { ...this.custom(), [edge]: value };
    this.custom.set(next);

    if (!next.start || !next.end) {
      this.customError.set(null);
      return;
    }
    if (!isValidRange(next)) {
      this.customError.set('A data inicial deve ser anterior ou igual à final.');
      return;
    }
    this.customError.set(null);
    this.range.set(next);
    this.load();
  }

  greetingName(): string {
    const email = this.auth.email();
    return email ? email.split('@')[0] : 'por aqui';
  }

  balanceTone(): 'positive' | 'negative' | 'neutral' {
    const balance = this.data()?.summary.balance ?? 0;
    if (balance > 0) return 'positive';
    if (balance < 0) return 'negative';
    return 'neutral';
  }

  private percentView(current: number, previous: number, upIsGood: boolean): DeltaView {
    const delta = compare(current, previous);
    const against = this.previousLabel();

    if (delta.direction === 'none') {
      return { arrow: '', text: 'Sem base de comparação', tone: 'neutral',
        sr: `Sem base de comparação: não houve movimentação em ${against}` };
    }
    if (delta.direction === 'flat') {
      return { arrow: '=', text: 'Sem variação', tone: 'neutral', sr: `Sem variação em relação a ${against}` };
    }
    const up = delta.direction === 'up';
    const size = `${Math.abs(delta.percent ?? 0).toLocaleString('pt-BR', { maximumFractionDigits: 1 })}%`;
    return {
      arrow: up ? '▲' : '▼',
      text: size,
      tone: up === upIsGood ? 'good' : 'bad',
      sr: `${up ? 'Aumento' : 'Queda'} de ${size} em relação a ${against}`,
    };
  }

  private balanceView(current: number, previous: number): DeltaView {
    const absolute = current - previous;
    const against = this.previousLabel();

    if (Math.abs(absolute) < 0.005) {
      return { arrow: '=', text: 'Sem variação', tone: 'neutral', sr: `Sem variação em relação a ${against}` };
    }
    const up = absolute > 0;
    const size = this.money.transform(Math.abs(absolute));
    return {
      arrow: up ? '▲' : '▼',
      text: size,
      tone: up ? 'good' : 'bad',
      sr: `${up ? 'Aumento' : 'Queda'} de ${size} em relação a ${against}`,
    };
  }
}
