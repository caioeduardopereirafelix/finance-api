import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';

describe('App (aviso de e-mail não confirmado)', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    vi.stubGlobal('matchMedia', () => ({
      matches: false, addEventListener: () => undefined, removeEventListener: () => undefined,
    }));
    localStorage.setItem('finance.accessToken', 'a');
    localStorage.setItem('finance.refreshToken', 'r');
    localStorage.setItem('finance.email', 'caio@exemplo.com');
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    localStorage.clear();
    http.verify();
    vi.unstubAllGlobals();
  });

  const start = (emailVerified: boolean) => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    http.expectOne('/account').flush({ name: 'Caio', email: 'caio@exemplo.com', emailVerified });
    fixture.detectChanges();
    return fixture;
  };

  it('mostra o aviso e reenvia o e-mail pelo botão', () => {
    const fixture = start(false);
    const el = fixture.nativeElement as HTMLElement;

    expect(el.querySelector('.verify-banner')!.textContent).toContain('caio@exemplo.com');

    el.querySelector<HTMLButtonElement>('.verify-banner button')!.click();
    http.expectOne('/account/email-verification').flush(null, { status: 202, statusText: 'Accepted' });
    fixture.detectChanges();

    expect(el.querySelector('.verify-banner')).not.toBeNull();
  });

  it('não mostra o aviso para quem já confirmou', () => {
    const fixture = start(true);

    expect((fixture.nativeElement as HTMLElement).querySelector('.verify-banner')).toBeNull();
  });
});
