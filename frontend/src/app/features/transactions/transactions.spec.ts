import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Transaction } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { TransactionsPage } from './transactions';

const bank: Transaction = {
  id: 'b-1', description: 'Tarifa desconhecida', amount: 9.9, category: 'OTHER_EXPENSE', type: 'EXPENSES',
  occurredAt: '2026-09-20T12:00:00Z', source: 'BANK', createdDate: '2026-09-20T12:00:00Z',
};
const manual: Transaction = {
  id: 'm-1', description: 'Café', amount: 8.5, category: 'FOOD', type: 'EXPENSES',
  occurredAt: '2026-09-21T12:00:00Z', source: 'MANUAL', createdDate: '2026-09-21T12:00:00Z',
};

describe('TransactionsPage (categoria)', () => {
  let fixture: ComponentFixture<TransactionsPage>;
  let page: TransactionsPage;
  let http: HttpTestingController;
  let el: HTMLElement;

  const flushList = (content: Transaction[]) => {
    http.expectOne(r => r.url === '/transaction').flush({
      content, totalElements: content.length, totalPages: 1, number: 0, size: 10,
    });
    fixture.detectChanges();
  };

  beforeEach(async () => {
    // o jsdom pode nao ter implementado <dialog>: o teste nao depende de abrir de verdade
    const proto = HTMLDialogElement.prototype as unknown as Record<string, unknown>;
    proto['showModal'] ??= function () { /* noop */ };
    proto['close'] ??= function () { /* noop */ };

    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TransactionsPage);
    page = fixture.componentInstance;
    el = fixture.nativeElement;
    fixture.detectChanges();
    flushList([bank, manual]);
    await fixture.whenStable();
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  const rowOf = (text: string) =>
    Array.from(el.querySelectorAll('tbody tr')).find(tr => tr.textContent?.includes(text))!;

  it('a transação importada oferece só Categoria; a manual continua com Editar', () => {
    const importada = rowOf('Tarifa desconhecida').textContent!;
    expect(importada).toContain('Categoria');
    expect(importada).not.toContain('Editar');

    const digitada = rowOf('Café').textContent!;
    expect(digitada).toContain('Editar');
    expect(digitada).not.toContain('Categoria da transação');
  });

  it('o diálogo só oferece categorias do mesmo tipo da transação', () => {
    page.openCategory(bank);
    fixture.detectChanges();

    const options = Array.from(el.querySelectorAll('#cat-select option')).map(o => o.textContent!.trim());
    expect(options).toContain('Alimentação');
    expect(options).toContain('Outras despesas');
    expect(options).not.toContain('Salário');
    expect(page.applyToSimilar()).toBe(true);
  });

  it('salvar manda o PATCH, avisa quantas mudaram e recarrega a lista', () => {
    const notices = TestBed.inject(NotificationService);
    page.openCategory(bank);
    page.newCategory.set('BILLS');

    page.saveCategory();

    const req = http.expectOne('/transaction/b-1/category');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ category: 'BILLS', applyToSimilar: true });
    req.flush({ transaction: { ...bank, category: 'BILLS' }, updated: 3 });

    expect(notices.notices().some(n => n.text === 'Categoria alterada em 3 transações.')).toBe(true);
    flushList([{ ...bank, category: 'BILLS' }, manual]);   // recarregou
  });

  it('no singular, quando só uma mudou', () => {
    const notices = TestBed.inject(NotificationService);
    page.openCategory(bank);
    page.newCategory.set('BILLS');
    page.applyToSimilar.set(false);

    page.saveCategory();

    const req = http.expectOne('/transaction/b-1/category');
    expect(req.request.body).toEqual({ category: 'BILLS', applyToSimilar: false });
    req.flush({ transaction: { ...bank, category: 'BILLS' }, updated: 1 });

    expect(notices.notices().some(n => n.text === 'Categoria alterada em 1 transação.')).toBe(true);
    flushList([bank, manual]);
  });

  it('sem mudar a categoria e sem aplicar às parecidas, não chama a API', () => {
    page.openCategory(bank);
    page.applyToSimilar.set(false);

    page.saveCategory();

    http.expectNone('/transaction/b-1/category');
  });

  it('mesmo sem mudar a categoria, aplicar às parecidas é uma ação válida', () => {
    page.openCategory(bank);   // mantém OTHER_EXPENSE, com "aplicar às parecidas" marcado

    page.saveCategory();

    const req = http.expectOne('/transaction/b-1/category');
    expect(req.request.body).toEqual({ category: 'OTHER_EXPENSE', applyToSimilar: true });
    req.flush({ transaction: bank, updated: 1 });
    flushList([bank, manual]);
  });

  it('se a API recusar, mostra o motivo', () => {
    const notices = TestBed.inject(NotificationService);
    page.openCategory(bank);
    page.newCategory.set('BILLS');

    page.saveCategory();

    http.expectOne('/transaction/b-1/category').flush(
      { status: 422, error: 'erro de validacao', fieldsError: [{ field: 'category', message: 'Category does not match the transaction type' }] },
      { status: 422, statusText: 'Unprocessable Entity' });

    expect(notices.notices().some(n => n.tone === 'error')).toBe(true);
  });
});
