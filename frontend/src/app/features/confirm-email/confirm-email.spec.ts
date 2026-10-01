import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';

import { ConfirmEmailPage } from './confirm-email';

describe('ConfirmEmailPage', () => {
  let fixture: ComponentFixture<ConfirmEmailPage>;
  let http: HttpTestingController;
  let el: HTMLElement;

  const setup = (fragment: string | null) => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { fragment } } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ConfirmEmailPage);
    el = fixture.nativeElement;
    fixture.detectChanges();
  };

  afterEach(() => http.verify());

  it('sem token no link mostra o aviso e não chama o servidor', () => {
    setup(null);

    http.expectNone('/v1/auth/verify-email');
    expect(el.querySelector('h1')!.textContent).toContain('Link inválido ou expirado');
  });

  it('confirma sozinho ao abrir e mostra o sucesso', () => {
    setup('token=abc-123');
    expect(el.querySelector('h1')!.textContent).toContain('Confirmando');

    const req = http.expectOne('/v1/auth/verify-email');
    expect(req.request.body).toEqual({ token: 'abc-123' });
    req.flush(null, { status: 204, statusText: 'No Content' });
    fixture.detectChanges();

    expect(el.querySelector('h1')!.textContent).toContain('E-mail confirmado');
    expect(el.querySelector('a[href="/entrar"]')).not.toBeNull();
  });

  it('link recusado pelo servidor mostra link inválido', () => {
    setup('token=velho');

    http.expectOne('/v1/auth/verify-email')
      .flush({ status: 400, error: 'Invalid or expired email verification link', fieldsError: [] },
        { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(el.querySelector('h1')!.textContent).toContain('Link inválido ou expirado');
  });

  it('falha de rede mostra o erro e permite tentar de novo', () => {
    setup('token=abc');

    http.expectOne('/v1/auth/verify-email').error(new ProgressEvent('error'), { status: 0 });
    fixture.detectChanges();
    expect(el.querySelector('h1')!.textContent).toContain('Não foi possível confirmar');

    el.querySelector<HTMLButtonElement>('button')!.click();
    http.expectOne('/v1/auth/verify-email').flush(null, { status: 204, statusText: 'No Content' });
    fixture.detectChanges();

    expect(el.querySelector('h1')!.textContent).toContain('E-mail confirmado');
  });
});
