import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { serverStatusInterceptor } from './server-status.interceptor';
import { SLOW_RESPONSE_MS, ServerStatusService } from './server-status.service';

describe('Aviso de servidor acordando', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let status: ServerStatusService;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([serverStatusInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    status = TestBed.inject(ServerStatusService);
  });

  afterEach(() => {
    backend.verify();
    vi.useRealTimers();
  });

  it('não avisa quando a resposta chega rápido', () => {
    http.get('/account').subscribe();
    vi.advanceTimersByTime(SLOW_RESPONSE_MS - 1);
    backend.expectOne('/account').flush({});
    vi.advanceTimersByTime(SLOW_RESPONSE_MS);

    expect(status.waking()).toBe(false);
  });

  it('avisa quando a primeira resposta demora e some quando ela chega', () => {
    http.get('/account').subscribe();
    const req = backend.expectOne('/account');

    vi.advanceTimersByTime(SLOW_RESPONSE_MS);
    expect(status.waking()).toBe(true);

    req.flush({});
    expect(status.waking()).toBe(false);
  });

  it('um erro 4xx também mostra que o servidor está de pé', () => {
    http.get('/account').subscribe({ error: () => undefined });
    const req = backend.expectOne('/account');

    vi.advanceTimersByTime(SLOW_RESPONSE_MS);
    req.flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(status.waking()).toBe(false);
  });

  it('um erro 5xx mantém o aviso', () => {
    http.get('/account').subscribe({ error: () => undefined });
    const req = backend.expectOne('/account');

    vi.advanceTimersByTime(SLOW_RESPONSE_MS);
    req.flush({}, { status: 503, statusText: 'Service Unavailable' });

    expect(status.waking()).toBe(true);
  });

  it('depois de acordado, requisições lentas não disparam o aviso', () => {
    http.get('/ping').subscribe();
    backend.expectOne('/ping').flush({});

    http.get('/bank/sync').subscribe();
    const req = backend.expectOne('/bank/sync');
    vi.advanceTimersByTime(SLOW_RESPONSE_MS * 3);

    expect(status.waking()).toBe(false);
    req.flush({});
  });
});
