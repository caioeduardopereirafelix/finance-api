import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { TransactionService } from './transaction.service';

describe('TransactionService (resumos)', () => {
  let service: TransactionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(TransactionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('o resumo leva o período como startDate e endDate', () => {
    service.summary({ start: '2026-09-01', end: '2026-09-29' }).subscribe();

    const req = http.expectOne(r => r.url === '/transaction/summary');
    expect(req.request.params.get('startDate')).toBe('2026-09-01');
    expect(req.request.params.get('endDate')).toBe('2026-09-29');
    req.flush({ cashEntry: 0, expenses: 0, balance: 0 });
  });

  it('sem período o resumo não manda datas', () => {
    service.summary().subscribe();

    const req = http.expectOne(r => r.url === '/transaction/summary');
    expect(req.request.params.keys()).toEqual([]);
    req.flush({ cashEntry: 0, expenses: 0, balance: 0 });
  });

  it('o total por categoria usa a rota própria e o período', () => {
    let total = 0;
    service.categoryTotals({ start: '2026-09-01', end: '2026-09-29' }).subscribe(r => (total = r.length));

    const req = http.expectOne(r => r.url === '/transaction/summary/by-category');
    expect(req.request.params.get('startDate')).toBe('2026-09-01');
    req.flush([{ category: 'FOOD', type: 'EXPENSES', total: 10, count: 1 }]);

    expect(total).toBe(1);
  });
});

describe('TransactionService (categoria)', () => {
  let service: TransactionService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(TransactionService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('troca a categoria com PATCH e diz se vale para as parecidas', () => {
    let updated = 0;
    service.updateCategory('t-1', 'BILLS', true).subscribe(r => (updated = r.updated));

    const req = http.expectOne('/transaction/t-1/category');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ category: 'BILLS', applyToSimilar: true });
    req.flush({ transaction: { id: 't-1', category: 'BILLS' }, updated: 3 });

    expect(updated).toBe(3);
  });
});
