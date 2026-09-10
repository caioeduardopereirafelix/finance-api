import { Component, inject } from '@angular/core';

import { NotificationService } from '../core/notification.service';

/**
 * Regiao de status (WCAG 4.1.3).
 *
 * Sao duas regioes de proposito: `polite` para sucesso/informacao, que espera
 * o leitor de tela terminar a frase atual, e `assertive` para erro, que
 * interrompe. Misturar os dois numa regiao so faria o erro ser anunciado
 * tarde demais.
 */
@Component({
  selector: 'app-toast-region',
  template: `
    <div class="toast-stack">
      <div aria-live="polite" aria-atomic="false" class="toast-slot">
        @for (notice of polite(); track notice.id) {
          <div class="toast toast-{{ notice.tone }}">
            <span class="toast-icon" aria-hidden="true">{{ notice.tone === 'success' ? '✓' : 'i' }}</span>
            <p class="toast-text">{{ notice.text }}</p>
            <button type="button" class="toast-close" (click)="notifications.dismiss(notice.id)">
              <span aria-hidden="true">×</span>
              <span class="sr-only">Fechar aviso: {{ notice.text }}</span>
            </button>
          </div>
        }
      </div>

      <div role="alert" aria-live="assertive" aria-atomic="false" class="toast-slot">
        @for (notice of errors(); track notice.id) {
          <div class="toast toast-error">
            <span class="toast-icon" aria-hidden="true">⚠</span>
            <p class="toast-text">{{ notice.text }}</p>
            <button type="button" class="toast-close" (click)="notifications.dismiss(notice.id)">
              <span aria-hidden="true">×</span>
              <span class="sr-only">Fechar erro: {{ notice.text }}</span>
            </button>
          </div>
        }
      </div>
    </div>
  `,
  styles: [`
    .toast-stack {
      position: fixed;
      inset-block-end: 1rem;
      inset-inline: 1rem;
      z-index: 60;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      pointer-events: none;
    }
    .toast-slot { display: flex; flex-direction: column; gap: 0.5rem; }
    .toast {
      pointer-events: auto;
      display: flex;
      align-items: flex-start;
      gap: 0.6rem;
      max-width: 34rem;
      margin-inline: auto;
      width: 100%;
      padding: 0.75rem 0.9rem;
      border-radius: var(--radius);
      border: 1px solid var(--border);
      background: var(--surface);
      box-shadow: var(--shadow);
    }
    .toast-success { border-left: 4px solid var(--income); }
    .toast-error   { border-left: 4px solid var(--expense); }
    .toast-info    { border-left: 4px solid var(--primary); }
    .toast-icon {
      flex: none; font-weight: 700; line-height: 1.6;
    }
    .toast-success .toast-icon { color: var(--income); }
    .toast-error .toast-icon { color: var(--expense); }
    .toast-info .toast-icon { color: var(--primary); }
    .toast-text { margin: 0; flex: 1; }
    .toast-close {
      flex: none;
      min-width: 44px; min-height: 44px;
      display: inline-flex; align-items: center; justify-content: center;
      background: transparent; border: 1px solid transparent;
      border-radius: var(--radius);
      color: var(--text); font-size: 1.25rem; cursor: pointer;
    }
    .toast-close:hover { background: var(--surface-2); }
  `],
})
export class ToastRegion {
  readonly notifications = inject(NotificationService);

  polite = () => this.notifications.notices().filter(n => n.tone !== 'error');
  errors = () => this.notifications.notices().filter(n => n.tone === 'error');
}
