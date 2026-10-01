import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';

import { AuthService } from '../../core/auth.service';
import { NotificationService } from '../../core/notification.service';
import { ResetPasswordPage } from './reset-password';

describe('ResetPasswordPage', () => {
  let fixture: ComponentFixture<ResetPasswordPage>;
  let page: ResetPasswordPage;
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
    fixture = TestBed.createComponent(ResetPasswordPage);
    page = fixture.componentInstance;
    el = fixture.nativeElement;
    fixture.detectChanges();
  };

  afterEach(() => http.verify());

  const fill = (password: string, confirm: string) => {
    page.form.setValue({ password, confirm });
    page.submit();
    fixture.detectChanges();
  };

  it('sem token no link mostra o aviso e nenhum formulário', () => {
    setup(null);

    expect(el.querySelector('h1')!.textContent).toContain('Link inválido ou expirado');
    expect(el.querySelector('form')).toBeNull();
    expect(el.querySelector('a[href="/esqueci-senha"]')).not.toBeNull();
  });

  it('senhas diferentes não chegam ao servidor', () => {
    setup('token=abc');

    fill('senha-nova-123', 'senha-nova-124');

    http.expectNone('/v1/auth/reset-password');
    expect(el.querySelector('.error-summary')!.textContent).toContain('As senhas não conferem.');
  });

  it('senha curta não chega ao servidor', () => {
    setup('token=abc');

    fill('curta', 'curta');

    http.expectNone('/v1/auth/reset-password');
    expect(el.querySelector('.error-summary')!.textContent).toContain('ao menos 8 caracteres');
  });

  it('envia o token do link, avisa e encerra a sessão local ao concluir', () => {
    setup('token=abc-123');
    const forceLogout = vi.spyOn(TestBed.inject(AuthService), 'forceLogout').mockImplementation(() => undefined);

    fill('senha-nova-123', 'senha-nova-123');

    const req = http.expectOne('/v1/auth/reset-password');
    expect(req.request.body).toEqual({ token: 'abc-123', password: 'senha-nova-123' });
    req.flush(null, { status: 204, statusText: 'No Content' });

    expect(forceLogout).toHaveBeenCalled();
    expect(TestBed.inject(NotificationService).notices().map(n => n.text))
      .toContain('Senha alterada. Entre com a nova senha.');
  });

  it('link recusado pelo servidor troca o formulário pelo aviso de link inválido', () => {
    setup('token=velho');

    fill('senha-nova-123', 'senha-nova-123');
    http.expectOne('/v1/auth/reset-password')
      .flush({ status: 400, error: 'Invalid or expired password reset link', fieldsError: [] },
        { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(el.querySelector('h1')!.textContent).toContain('Link inválido ou expirado');
    expect(el.querySelector('form')).toBeNull();
  });
});
