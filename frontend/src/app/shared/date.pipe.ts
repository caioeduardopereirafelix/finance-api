import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'brDate' })
export class BrDatePipe implements PipeTransform {

  private readonly formatter = new Intl.DateTimeFormat('pt-BR', {
    day: '2-digit', month: '2-digit', year: 'numeric',
  });

  transform(iso: string | null | undefined): string {
    if (!iso) return '—';
    const date = new Date(iso);
    return Number.isNaN(date.getTime()) ? '—' : this.formatter.format(date);
  }
}
