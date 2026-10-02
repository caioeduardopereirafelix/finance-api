import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from './api.config';
import { CategoryChange, CategoryName, CategoryTotal, ImportResult, PageResponse, Summary, Transaction, TransactionFilters, TransactionPayload } from './models';

@Injectable({ providedIn: 'root' })
export class TransactionService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(filters: TransactionFilters): Observable<PageResponse<Transaction>> {
    let params = new HttpParams();

    const set = (key: string, value: unknown) => {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    };

    set('type', filters.type);
    set('category', filters.category);
    set('description', filters.description);
    set('minAmount', filters.minAmount);
    set('maxAmount', filters.maxAmount);
    set('startDate', filters.startDate);
    set('endDate', filters.endDate);
    set('page', filters.page ?? 0);
    set('size', filters.size ?? 10);

    return this.http.get<PageResponse<Transaction>>(`${this.baseUrl}/transaction`, { params });
  }

  importStatement(file: File, invertSign: boolean): Observable<ImportResult> {
    const form = new FormData();
    form.append('file', file, file.name);
    form.append('invertSign', String(invertSign));
    return this.http.post<ImportResult>(`${this.baseUrl}/transaction/import`, form);
  }

  getById(id: string): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.baseUrl}/transaction/${id}`);
  }

  create(payload: TransactionPayload): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.baseUrl}/transaction`, payload);
  }

  update(id: string, payload: TransactionPayload): Observable<Transaction> {
    return this.http.put<Transaction>(`${this.baseUrl}/transaction/${id}`, payload);
  }

  updateCategory(id: string, category: CategoryName, applyToSimilar: boolean): Observable<CategoryChange> {
    return this.http.patch<CategoryChange>(`${this.baseUrl}/transaction/${id}/category`, { category, applyToSimilar });
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/transaction/${id}`);
  }

  summary(range?: { start: string; end: string }): Observable<Summary> {
    return this.http.get<Summary>(`${this.baseUrl}/transaction/summary`, { params: this.periodParams(range) });
  }

  categoryTotals(range?: { start: string; end: string }): Observable<CategoryTotal[]> {
    return this.http.get<CategoryTotal[]>(`${this.baseUrl}/transaction/summary/by-category`,
      { params: this.periodParams(range) });
  }

  private periodParams(range?: { start: string; end: string }): HttpParams {
    let params = new HttpParams();
    if (range) {
      params = params.set('startDate', range.start).set('endDate', range.end);
    }
    return params;
  }
}
