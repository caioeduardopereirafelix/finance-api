import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { RegisterPage } from './register';

describe('RegisterPage (e-mail)', () => {
  let fixture: ComponentFixture<RegisterPage>;
  let page: RegisterPage;
  let http: HttpTestingController;
  let el: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([{ path: 'entrar', children: [] }])],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(RegisterPage);
    page = fixture.componentInstance;
    el = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => http.verify());

  it('e-mail sem domínio completo não chega ao servidor', () => {
    page.form.setValue({ name: 'Ana', email: 'afafasf@gfsgsg', password: 'senha-segura-1' });
    page.submit();
    fixture.detectChanges();

    http.expectNone('/v1/auth/register');
    expect(el.querySelector('.error-summary')!.textContent).toContain('e-mail válido');
  });

  it('e-mail completo segue para o servidor', () => {
    page.form.setValue({ name: 'Ana', email: 'ana@exemplo.com', password: 'senha-segura-1' });
    page.submit();

    const req = http.expectOne('/v1/auth/register');
    expect(req.request.body.email).toBe('ana@exemplo.com');
    req.flush(null, { status: 201, statusText: 'Created' });
    http.expectOne('/v1/auth/login').flush(null, { status: 401, statusText: 'Unauthorized' });
  });
});
