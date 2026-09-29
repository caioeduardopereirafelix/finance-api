import { Component, computed, input, signal } from '@angular/core';

import { CategoryName, CategoryTotal, TransactionalType } from '../../core/models';
import { MoneyPipe } from '../../shared/money.pipe';
import { Bar, buildBars, formatShare } from './category-bars';

@Component({
  selector: 'app-category-chart',
  imports: [MoneyPipe],
  templateUrl: './category-chart.html',
  styleUrl: './category-chart.css',
})
export class CategoryChart {

  readonly items = input.required<CategoryTotal[]>();
  readonly periodLabel = input('');
  readonly refreshing = input(false);

  readonly type = signal<TransactionalType>('EXPENSES');
  readonly showTable = signal(false);
  readonly active = signal<CategoryName | null>(null);

  readonly bars = computed(() => buildBars(this.items(), this.type()));
  readonly total = computed(() => this.bars().reduce((sum, bar) => sum + bar.total, 0));
  readonly noun = computed(() => (this.type() === 'EXPENSES' ? 'saídas' : 'entradas'));

  setType(type: TransactionalType) {
    this.type.set(type);
    this.active.set(null);
  }

  share(bar: Bar): string {
    return formatShare(bar.share);
  }

  countText(bar: Bar): string {
    return `${bar.count} ${bar.count === 1 ? 'transação' : 'transações'}`;
  }
}
