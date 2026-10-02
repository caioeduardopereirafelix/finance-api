import { Injectable, signal } from '@angular/core';

export type NoticeTone = 'success' | 'error' | 'info';

export interface Notice {
  id: number;
  tone: NoticeTone;
  text: string;
}

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
    if (tone !== 'error') {
      setTimeout(() => this.dismiss(id), 6000);
    }
  }
}
