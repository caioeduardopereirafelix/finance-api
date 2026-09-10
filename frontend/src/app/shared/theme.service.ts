import { Injectable, signal } from '@angular/core';

export type Theme = 'light' | 'dark' | 'system';

const KEY = 'finance.theme';

@Injectable({ providedIn: 'root' })
export class ThemeService {

  readonly theme = signal<Theme>(read());

  constructor() {
    this.apply(this.theme());
  }

  set(theme: Theme) {
    this.theme.set(theme);
    try { localStorage.setItem(KEY, theme); } catch { /* modo privado */ }
    this.apply(theme);
  }

  /** Alterna claro <-> escuro partindo do que o sistema indica. */
  toggle() {
    this.set(this.resolved() === 'dark' ? 'light' : 'dark');
  }

  resolved(): 'light' | 'dark' {
    const current = this.theme();
    if (current !== 'system') return current;
    return matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  }

  private apply(theme: Theme) {
    const root = document.documentElement;
    if (theme === 'system') {
      root.removeAttribute('data-theme');
    } else {
      root.setAttribute('data-theme', theme);
    }
  }
}

function read(): Theme {
  try {
    const stored = localStorage.getItem(KEY);
    return stored === 'light' || stored === 'dark' ? stored : 'system';
  } catch {
    return 'system';
  }
}
