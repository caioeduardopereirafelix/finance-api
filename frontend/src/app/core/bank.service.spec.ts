import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { BankService } from './bank.service';

describe('BankService', () => {
  let service: BankService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(BankService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('pede o token do widget e recebe o nome do provedor', () => {
    let provider = '';
    service.connectToken().subscribe(r => (provider = r.provider));

    const req = http.expectOne('/bank/connect-token');
    expect(req.request.method).toBe('POST');
    req.flush({ token: 't', provider: 'mock' });

    expect(provider).toBe('mock');
  });

  it('registra a conexao com o externalId', () => {
    service.connect('abc').subscribe();

    const req = http.expectOne('/bank/connections');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ externalId: 'abc' });
    req.flush({});
  });

  it('desconectar sem deleteImported nao manda o parametro', () => {
    service.disconnect('1', false).subscribe();

    const req = http.expectOne('/bank/connections/1');
    expect(req.request.method).toBe('DELETE');
    expect(req.request.params.has('deleteImported')).toBe(false);
    req.flush(null);
  });

  it('desconectar com deleteImported manda deleteImported=true', () => {
    service.disconnect('1', true).subscribe();

    const req = http.expectOne('/bank/connections/1?deleteImported=true');
    expect(req.request.params.get('deleteImported')).toBe('true');
    req.flush(null);
  });

  it('sincroniza pela rota da conexao', () => {
    service.sync('9').subscribe();
    http.expectOne('/bank/connections/9/sync').flush({ imported: 0, skipped: 0 });
  });
});
