import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, tap, throwError } from 'rxjs';
import { catchError, finalize, shareReplay } from 'rxjs/operators';

import { API_BASE_URL } from './api.config';
import { AuthResponse } from './models';

const ACCESS_KEY = 'finance.accessToken';
const REFRESH_KEY = 'finance.refreshToken';
const EMAIL_KEY = 'finance.email';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly baseUrl = inject(API_BASE_URL);

  readonly accessToken = signal<string | null>(read(ACCESS_KEY));
  readonly refreshToken = signal<string | null>(read(REFRESH_KEY));
  readonly email = signal<string | null>(read(EMAIL_KEY));

  readonly isLoggedIn = computed(() => this.accessToken() !== null);

  /** Refresh em voo, compartilhado: varias requisicoes que tomam 401 ao mesmo
   *  tempo aguardam a mesma renovacao em vez de gastarem o token cada uma. */
  private refreshInFlight: Observable<AuthResponse> | null = null;

  register(email: string, name: string, password: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/v1/auth/register`, {
      email, user: name, password,
    });
  }

  login(email: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.baseUrl}/v1/auth/login`, { email, password })
      .pipe(tap(res => this.store(res, email)));
  }

  refresh(): Observable<AuthResponse> {
    if (this.refreshInFlight) {
      return this.refreshInFlight;
    }

    const token = this.refreshToken();
    if (!token) {
      return throwError(() => new Error('Sem refresh token disponível'));
    }

    this.refreshInFlight = this.http
      .post<AuthResponse>(`${this.baseUrl}/v1/auth/refresh`, { refreshToken: token })
      .pipe(
        tap(res => this.store(res, this.email())),
        catchError(err => {
          this.clear();
          return throwError(() => err);
        }),
        finalize(() => { this.refreshInFlight = null; }),
        shareReplay(1),
      );

    return this.refreshInFlight;
  }

  logout(): void {
    const token = this.refreshToken();

    const finish = () => {
      this.clear();
      this.router.navigate(['/entrar']);
    };

    if (!token) {
      finish();
      return;
    }

    // Revoga no servidor; mesmo se falhar, a sessao local termina.
    this.http.post<void>(`${this.baseUrl}/v1/auth/logout`, { refreshToken: token })
      .subscribe({ next: finish, error: finish });
  }

  /** Sessao encerrada pelo interceptor quando a renovacao nao e possivel. */
  forceLogout(): void {
    this.clear();
    this.router.navigate(['/entrar']);
  }

  private store(res: AuthResponse, email: string | null) {
    this.accessToken.set(res.token);
    this.refreshToken.set(res.refreshToken);
    write(ACCESS_KEY, res.token);
    write(REFRESH_KEY, res.refreshToken);
    if (email) {
      this.email.set(email);
      write(EMAIL_KEY, email);
    }
  }

  private clear() {
    this.accessToken.set(null);
    this.refreshToken.set(null);
    this.email.set(null);
    [ACCESS_KEY, REFRESH_KEY, EMAIL_KEY].forEach(k => {
      try { localStorage.removeItem(k); } catch { /* modo privado */ }
    });
  }
}

function read(key: string): string | null {
  try { return localStorage.getItem(key); } catch { return null; }
}

function write(key: string, value: string): void {
  try { localStorage.setItem(key, value); } catch { /* modo privado */ }
}
