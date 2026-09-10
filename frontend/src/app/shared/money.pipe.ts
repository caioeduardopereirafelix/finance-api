import { Pipe, PipeTransform } from '@angular/core';

/** Formata em real brasileiro. Valor nulo vira traco, nao "NaN". */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {

  private readonly formatter = new Intl.NumberFormat('pt-BR', {
    style: 'currency',
    currency: 'BRL',
  });

  transform(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
      return '—';
    }
    return this.formatter.format(value);
  }
}
