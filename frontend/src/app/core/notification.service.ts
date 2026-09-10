import { Injectable, signal } from '@angular/core';

export type NoticeTone = 'success' | 'error' | 'info';

export interface Notice {
  id: number;
  tone: NoticeTone;
  text: string;
}

/**
 * Mensagens de status da aplicacao.
 *
 * Sao renderizadas numa regiao aria-live (ver ToastRegion), o que atende ao
 * criterio 4.1.3 do WCAG: o leitor de tela anuncia o resultado da acao sem
 * que o foco precise sair de onde esta.
 */
@Injectable({ providedIn: 'root' })
export class NotificationService {

  private nextId = 0;
  readonly notices = signal<Notice[]>([]);

  success(text: string) { this.push('success', text); }
  error(text: string) { this.push('error', text); }
  info(text: string) { this.push('info', text); }

  dismiss(id: number) {
    this.notices.update(list => list.filter(n => n.id !== id));
  }

  private push(tone: NoticeTone, text: string) {
    const id = this.nextId++;
    this.notices.update(list => [...list, { id, tone, text }]);
    // Erros permanecem ate o usuario fechar: quem le devagar nao perde a mensagem.
    if (tone !== 'error') {
      setTimeout(() => this.dismiss(id), 6000);
    }
  }
}
