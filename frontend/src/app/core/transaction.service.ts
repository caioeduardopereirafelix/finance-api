import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from './api.config';
import { PageResponse, Summary, Transaction, TransactionFilters, TransactionPayload } from './models';

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

  getById(id: string): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.baseUrl}/transaction/${id}`);
  }

  create(payload: TransactionPayload): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.baseUrl}/transaction`, payload);
  }

  update(id: string, payload: TransactionPayload): Observable<Transaction> {
    return this.http.put<Transaction>(`${this.baseUrl}/transaction/${id}`, payload);
  }

  remove(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/transaction/${id}`);
  }

  summary(): Observable<Summary> {
    return this.http.get<Summary>(`${this.baseUrl}/transaction/summary`);
  }
}
