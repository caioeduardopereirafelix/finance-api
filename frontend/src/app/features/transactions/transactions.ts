import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { messageOf } from '../../core/api-error';
import {
  CATEGORIES_BY_TYPE, CATEGORY_LABEL, CategoryName,
  PageResponse, Transaction, TransactionalType, TransactionPayload, TYPE_LABEL,
} from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { TransactionService } from '../../core/transaction.service';
import { BrDatePipe } from '../../shared/date.pipe';
import { MoneyPipe } from '../../shared/money.pipe';
import { toIso } from '../dashboard/period';

const PAGE_SIZE = 10;
const EARLIEST_DATE = '2000-01-01';

@Component({
  selector: 'app-transactions',
  imports: [ReactiveFormsModule, MoneyPipe, BrDatePipe],
  templateUrl: './transactions.html',
  styleUrl: './transactions.css',
})
export class TransactionsPage {

  private readonly api = inject(TransactionService);
  private readonly fb = inject(FormBuilder);
  private readonly notifications = inject(NotificationService);

  private readonly formDialog = viewChild<ElementRef<HTMLDialogElement>>('formDialog');
  private readonly deleteDialog = viewChild<ElementRef<HTMLDialogElement>>('deleteDialog');
  private readonly categoryDialog = viewChild<ElementRef<HTMLDialogElement>>('categoryDialog');
  private readonly categorySelect = viewChild<ElementRef<HTMLSelectElement>>('categorySelect');
  private readonly descriptionInput = viewChild<ElementRef<HTMLInputElement>>('descriptionInput');

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly page = signal<PageResponse<Transaction> | null>(null);
  readonly filtersOpen = signal(false);
  readonly saving = signal(false);
  readonly editing = signal<Transaction | null>(null);
  readonly pendingDelete = signal<Transaction | null>(null);
  readonly recategorizing = signal<Transaction | null>(null);
  readonly newCategory = signal<CategoryName | null>(null);
  readonly applyToSimilar = signal(true);
  readonly formSubmitted = signal(false);
  readonly formError = signal<string | null>(null);

  readonly typeLabel = TYPE_LABEL;
  readonly categoryLabel = CATEGORY_LABEL;
  readonly allCategories = Object.keys(CATEGORY_LABEL) as CategoryName[];

  readonly filterForm = this.fb.nonNullable.group({
    description: [''],
    type: [''],
    category: [''],
    minAmount: [''],
    maxAmount: [''],
    startDate: [''],
    endDate: [''],
  });

  readonly form = this.fb.nonNullable.group({
    description: ['', [Validators.required, Validators.maxLength(255)]],
    amount: ['', [Validators.required]],
    type: ['CASH_ENTRY' as TransactionalType, [Validators.required]],
    category: ['WAGE' as CategoryName, [Validators.required]],
    date: [toIso(new Date())],
  });

  private currentPage = 0;
  private originalDate = '';

  constructor() {
    // Trocar o tipo troca as categorias validas; sem isso o backend recusaria.
    this.form.controls.type.valueChanges.subscribe(type => {
      const options = CATEGORIES_BY_TYPE[type];
      if (!options.includes(this.form.controls.category.value)) {
        this.form.controls.category.setValue(options[0]);
      }
    });

    this.load(0);
  }

  categoriesForSelectedType(): CategoryName[] {
    return CATEGORIES_BY_TYPE[this.form.controls.type.value];
  }

  load(pageIndex: number) {
    this.currentPage = pageIndex;
    this.loading.set(true);
    this.error.set(null);

    const raw = this.filterForm.getRawValue();

    this.api.list({
      description: raw.description.trim(),
      type: raw.type as TransactionalType | '',
      category: raw.category as CategoryName | '',
      minAmount: raw.minAmount ? Number(raw.minAmount) : null,
      maxAmount: raw.maxAmount ? Number(raw.maxAmount) : null,
      startDate: raw.startDate,
      endDate: raw.endDate,
      page: pageIndex,
      size: PAGE_SIZE,
    }).subscribe({
      next: (page) => {
        this.page.set(page);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(messageOf(err, 'Não foi possível carregar as transações.'));
        this.loading.set(false);
      },
    });
  }

  applyFilters() { this.load(0); }

  clearFilters() {
    this.filterForm.reset({
      description: '', type: '', category: '', minAmount: '', maxAmount: '', startDate: '', endDate: '',
    });
    this.load(0);
  }

  activeFilterCount(): number {
    return Object.values(this.filterForm.getRawValue()).filter(v => v !== '').length;
  }

  goToPage(index: number) { this.load(index); }

  resultSummary(): string {
    const page = this.page();
    if (!page) return '';
    if (page.totalElements === 0) return 'Nenhuma transação encontrada.';
    const from = page.number * page.size + 1;
    const to = from + page.content.length - 1;
    return `Mostrando ${from} a ${to} de ${page.totalElements} transações.`;
  }


  openCreate() {
    this.editing.set(null);
    this.formSubmitted.set(false);
    this.formError.set(null);
    this.originalDate = '';
    this.form.reset({ description: '', amount: '', type: 'CASH_ENTRY', category: 'WAGE', date: this.today() });
    this.openDialog(this.formDialog());
  }

  openEdit(transaction: Transaction) {
    this.editing.set(transaction);
    this.formSubmitted.set(false);
    this.formError.set(null);
    this.originalDate = toIso(new Date(transaction.occurredAt));
    this.form.reset({
      description: transaction.description,
      amount: String(transaction.amount),
      type: transaction.type,
      category: transaction.category,
      date: this.originalDate,
    });
    this.openDialog(this.formDialog());
  }

  closeForm() { this.formDialog()?.nativeElement.close(); }

  today(): string { return toIso(new Date()); }

  readonly earliestDate = EARLIEST_DATE;

  showFieldError(control: 'description' | 'amount'): boolean {
    return this.formSubmitted() && this.form.controls[control].invalid;
  }

  amountError(): string | null {
    if (!this.formSubmitted()) return null;
    const value = this.form.controls.amount.value;
    if (!value) return 'Informe o valor.';
    const parsed = Number(value.replace(',', '.'));
    if (Number.isNaN(parsed)) return 'Informe um número, como 1250,90.';
    if (parsed <= 0) return 'O valor deve ser maior que zero.';
    return null;
  }

  dateError(): string | null {
    if (!this.formSubmitted()) return null;
    const value = this.form.controls.date.value;
    if (!value) return 'Informe a data.';
    if (value > this.today()) return 'A data não pode ser futura.';
    if (value < EARLIEST_DATE) return 'Informe uma data a partir de 2000.';
    return null;
  }

  save() {
    this.formSubmitted.set(true);
    this.formError.set(null);

    if (this.form.controls.description.invalid || this.amountError() || this.dateError()) {
      return;
    }

    const raw = this.form.getRawValue();
    const payload: TransactionPayload = {
      description: raw.description.trim(),
      amount: Number(raw.amount.replace(',', '.')),
      type: raw.type,
      category: raw.category,
    };
    if (raw.date !== this.originalDate) {
      payload.occurredOn = raw.date;
    }

    this.saving.set(true);
    const editing = this.editing();
    const request = editing
      ? this.api.update(editing.id, payload)
      : this.api.create(payload);

    request.subscribe({
      next: () => {
        this.saving.set(false);
        this.closeForm();
        this.notifications.success(editing ? 'Transação atualizada.' : 'Transação adicionada.');
        this.load(editing ? this.currentPage : 0);
      },
      error: (err) => {
        this.saving.set(false);
        this.formError.set(messageOf(err, 'Não foi possível salvar a transação.'));
      },
    });
  }


  categoryChoices(transaction: Transaction | null): CategoryName[] {
    return transaction ? CATEGORIES_BY_TYPE[transaction.type] : [];
  }

  openCategory(transaction: Transaction) {
    this.recategorizing.set(transaction);
    this.newCategory.set(transaction.category);
    this.applyToSimilar.set(true);
    this.formError.set(null);

    const dialog = this.categoryDialog()?.nativeElement;
    if (!dialog) return;
    dialog.showModal();
    queueMicrotask(() => this.categorySelect()?.nativeElement.focus());
  }

  closeCategory() { this.categoryDialog()?.nativeElement.close(); }

  saveCategory() {
    const target = this.recategorizing();
    const category = this.newCategory();
    if (!target || !category) return;

    if (category === target.category && !this.applyToSimilar()) {
      this.closeCategory();
      return;
    }

    this.saving.set(true);
    this.api.updateCategory(target.id, category, this.applyToSimilar()).subscribe({
      next: (change) => {
        this.saving.set(false);
        this.closeCategory();
        this.notifications.success(change.updated === 1
          ? 'Categoria alterada em 1 transação.'
          : `Categoria alterada em ${change.updated} transações.`);
        this.load(this.currentPage);
      },
      error: (err) => {
        this.saving.set(false);
        this.closeCategory();
        this.notifications.error(messageOf(err, 'Não foi possível alterar a categoria.'));
      },
    });
  }

  askDelete(transaction: Transaction) {
    this.pendingDelete.set(transaction);
    this.openDialog(this.deleteDialog());
  }

  cancelDelete() { this.deleteDialog()?.nativeElement.close(); }

  confirmDelete() {
    const target = this.pendingDelete();
    if (!target) return;

    this.saving.set(true);
    this.api.remove(target.id).subscribe({
      next: () => {
        this.saving.set(false);
        this.cancelDelete();
        this.notifications.success(`Transação "${target.description}" excluída.`);
        const page = this.page();
        const lastItemOnPage = page?.content.length === 1 && this.currentPage > 0;
        this.load(lastItemOnPage ? this.currentPage - 1 : this.currentPage);
      },
      error: (err) => {
        this.saving.set(false);
        this.cancelDelete();
        this.notifications.error(messageOf(err, 'Não foi possível excluir a transação.'));
      },
    });
  }

  private openDialog(ref: ElementRef<HTMLDialogElement> | undefined) {
    const dialog = ref?.nativeElement;
    if (!dialog) return;
    dialog.showModal();
    queueMicrotask(() => this.descriptionInput()?.nativeElement.focus());
  }
}
