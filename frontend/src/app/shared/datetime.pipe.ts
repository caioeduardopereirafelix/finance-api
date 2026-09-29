import { Pipe, PipeTransform } from '@angular/core';

/** Data e hora no formato brasileiro, no fuso do navegador. */
@Pipe({ name: 'brDateTime' })
export class BrDateTimePipe implements PipeTransform {

  private readonly formatter = new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });

  transform(iso: string | null | undefined): string {
    if (!iso) return '—';
    const date = new Date(iso);
    return Number.isNaN(date.getTime()) ? '—' : this.formatter.format(date);
  }
}
