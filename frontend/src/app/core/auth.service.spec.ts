import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from './auth.service';

describe('AuthService (recuperação de senha)', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('o pedido de recuperação manda só o e-mail', () => {
    service.forgotPassword('caio@exemplo.com').subscribe();

    const req = http.expectOne('/v1/auth/forgot-password');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'caio@exemplo.com' });
    req.flush(null, { status: 202, statusText: 'Accepted' });
  });

  it('a redefinição manda o token e a nova senha', () => {
    service.resetPassword('tok-123', 'senha-nova-123').subscribe();

    const req = http.expectOne('/v1/auth/reset-password');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ token: 'tok-123', password: 'senha-nova-123' });
    req.flush(null, { status: 204, statusText: 'No Content' });
  });
});

describe('AuthService (confirmação de e-mail)', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('confirmar manda o token', () => {
    service.verifyEmail('tok-1').subscribe();

    const req = http.expectOne('/v1/auth/verify-email');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ token: 'tok-1' });
    req.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('o perfil guarda se o e-mail está confirmado', () => {
    expect(service.emailVerified()).toBeNull();

    service.loadProfile();
    http.expectOne('/account').flush({ name: 'Caio', email: 'caio@exemplo.com', emailVerified: false });

    expect(service.emailVerified()).toBe(false);
    expect(service.profile()?.email).toBe('caio@exemplo.com');
  });

  it('falha ao carregar o perfil deixa o estado desconhecido', () => {
    service.loadProfile();
    http.expectOne('/account').flush(null, { status: 500, statusText: 'Server Error' });

    expect(service.emailVerified()).toBeNull();
  });

  it('o reenvio usa a rota da conta', () => {
    service.resendVerification().subscribe();

    const req = http.expectOne('/account/email-verification');
    expect(req.request.method).toBe('POST');
    req.flush(null, { status: 202, statusText: 'Accepted' });
  });
});
