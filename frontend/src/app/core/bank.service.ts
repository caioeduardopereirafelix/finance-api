import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { API_BASE_URL } from './api.config';
import { BankConnection, BankSyncResult, ConnectToken } from './models';

@Injectable({ providedIn: 'root' })
export class BankService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  connectToken(): Observable<ConnectToken> {
    return this.http.post<ConnectToken>(`${this.baseUrl}/bank/connect-token`, {});
  }

  connect(externalId: string): Observable<BankConnection> {
    return this.http.post<BankConnection>(`${this.baseUrl}/bank/connections`, { externalId });
  }

  list(): Observable<BankConnection[]> {
    return this.http.get<BankConnection[]>(`${this.baseUrl}/bank/connections`);
  }

  sync(id: string): Observable<BankSyncResult> {
    return this.http.post<BankSyncResult>(`${this.baseUrl}/bank/connections/${id}/sync`, {});
  }

  disconnect(id: string, deleteImported: boolean): Observable<void> {
    let params = new HttpParams();
    if (deleteImported) {
      params = params.set('deleteImported', 'true');
    }
    return this.http.delete<void>(`${this.baseUrl}/bank/connections/${id}`, { params });
  }
}
