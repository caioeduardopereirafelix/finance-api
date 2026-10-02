import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { App } from './app';
import { ServerStatusService } from './core/server-status.service';

describe('App (aviso de servidor acordando)', () => {
  beforeEach(async () => {
    vi.stubGlobal('matchMedia', () => ({
      matches: false, addEventListener: () => undefined, removeEventListener: () => undefined,
    }));
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
  });

  afterEach(() => vi.unstubAllGlobals());

  it('mostra o aviso enquanto o servidor acorda e o esconde depois', () => {
    const fixture = TestBed.createComponent(App);
    const el = fixture.nativeElement as HTMLElement;
    const status = TestBed.inject(ServerStatusService);

    fixture.detectChanges();
    expect(el.querySelector('.server-banner')).toBeNull();

    status.waking.set(true);
    fixture.detectChanges();
    expect(el.querySelector('.server-banner')!.textContent).toContain('Acordando o servidor');

    status.markAwake();
    fixture.detectChanges();
    expect(el.querySelector('.server-banner')).toBeNull();
  });
});
