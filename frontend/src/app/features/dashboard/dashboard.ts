import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';

import { messageOf } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { CATEGORY_LABEL, Summary, Transaction, TYPE_LABEL } from '../../core/models';
import { TransactionService } from '../../core/transaction.service';
import { BrDatePipe } from '../../shared/date.pipe';
import { MoneyPipe } from '../../shared/money.pipe';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, MoneyPipe, BrDatePipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css',
})
export class DashboardPage {

  private readonly transactions = inject(TransactionService);
  readonly auth = inject(AuthService);

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly summary = signal<Summary | null>(null);
  readonly recent = signal<Transaction[]>([]);

  readonly typeLabel = TYPE_LABEL;
  readonly categoryLabel = CATEGORY_LABEL;

  constructor() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set(null);

    forkJoin({
      summary: this.transactions.summary(),
      page: this.transactions.list({ page: 0, size: 5 }),
    }).subscribe({
      next: ({ summary, page }) => {
        this.summary.set(summary);
        this.recent.set(page.content);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(messageOf(err, 'Não foi possível carregar seus dados.'));
        this.loading.set(false);
      },
    });
  }

  /** Primeiro nome, para a saudacao. */
  greetingName(): string {
    const email = this.auth.email();
    return email ? email.split('@')[0] : 'por aqui';
  }

  balanceTone(): 'positive' | 'negative' | 'neutral' {
    const balance = this.summary()?.balance ?? 0;
    if (balance > 0) return 'positive';
    if (balance < 0) return 'negative';
    return 'neutral';
  }
}
