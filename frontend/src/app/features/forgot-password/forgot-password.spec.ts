import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ForgotPasswordPage } from './forgot-password';

describe('ForgotPasswordPage', () => {
  let fixture: ComponentFixture<ForgotPasswordPage>;
  let page: ForgotPasswordPage;
  let http: HttpTestingController;
  let el: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ForgotPasswordPage);
    page = fixture.componentInstance;
    el = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('e-mail inválido não chega ao servidor', () => {
    page.form.setValue({ email: 'isto-nao-e-email' });
    page.submit();
    fixture.detectChanges();

    http.expectNone('/v1/auth/forgot-password');
    expect(el.querySelector('.error-summary')!.textContent).toContain('e-mail válido');
  });

  it('depois de enviar mostra a mesma confirmação sem dizer se a conta existe', () => {
    page.form.setValue({ email: 'caio@exemplo.com' });
    page.submit();

    http.expectOne('/v1/auth/forgot-password').flush(null, { status: 202, statusText: 'Accepted' });
    fixture.detectChanges();

    const text = el.textContent!;
    expect(el.querySelector('h1')!.textContent).toContain('Confira seu e-mail');
    expect(text).toContain('Se existir uma conta com o e-mail');
    expect(el.querySelector('form')).toBeNull();
  });

  it('falha de rede mostra o erro e mantém o formulário', () => {
    page.form.setValue({ email: 'caio@exemplo.com' });
    page.submit();

    http.expectOne('/v1/auth/forgot-password').error(new ProgressEvent('error'), { status: 0 });
    fixture.detectChanges();

    expect(el.querySelector('.error-summary')!.textContent).toContain('Não foi possível falar com o servidor');
    expect(el.querySelector('form')).not.toBeNull();
  });
});
