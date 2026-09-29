import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CategoryTotal } from '../../core/models';
import { CategoryChart } from './category-chart';

const items: CategoryTotal[] = [
  { category: 'FOOD', type: 'EXPENSES', total: 300, count: 5 },
  { category: 'TRANSPORT', type: 'EXPENSES', total: 100, count: 1 },
  { category: 'WAGE', type: 'CASH_ENTRY', total: 5000, count: 1 },
];

describe('CategoryChart', () => {
  let fixture: ComponentFixture<CategoryChart>;
  let el: HTMLElement;

  const render = async (data: CategoryTotal[] = items) => {
    fixture.componentRef.setInput('items', data);
    fixture.componentRef.setInput('periodLabel', '01/09/2026 a 29/09/2026');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  };

  beforeEach(() => {
    fixture = TestBed.createComponent(CategoryChart);
    el = fixture.nativeElement;
  });

  it('mostra as saídas por padrão, uma linha por categoria, com valor em texto', async () => {
    await render();

    expect(el.querySelector('h2')?.textContent).toContain('Saídas por categoria');
    const rows = el.querySelectorAll('.bar-row');
    expect(rows.length).toBe(2);
    expect(rows[0].textContent).toContain('Alimentação');
    expect(rows[0].textContent).toMatch(/R\$\s?300,00/);
  });

  it('cada linha tem um nome acessível com valor, participação e quantidade', async () => {
    await render();

    const label = el.querySelector('.bar-row')!.getAttribute('aria-label')!;
    expect(label).toContain('Alimentação');
    expect(label).toMatch(/R\$\s?300,00/);
    expect(label).toContain('75% das saídas');
    expect(label).toContain('5 transações');
  });

  it('a barra é decorativa: o valor não depende dela', async () => {
    await render();

    expect(el.querySelector('.bar')!.getAttribute('aria-hidden')).toBe('true');
  });

  it('mostra a dica ao focar a linha e some ao sair ou com Escape', async () => {
    await render();
    const row = el.querySelector('.bar-row') as HTMLElement;

    row.dispatchEvent(new Event('focus'));
    fixture.detectChanges();
    expect(el.querySelector('.tooltip')?.textContent).toContain('75% do total');

    row.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(el.querySelector('.tooltip')).toBeNull();

    row.dispatchEvent(new Event('focus'));
    fixture.detectChanges();
    row.dispatchEvent(new Event('blur'));
    fixture.detectChanges();
    expect(el.querySelector('.tooltip')).toBeNull();
  });

  it('troca para entradas', async () => {
    await render();

    (el.querySelectorAll('input[name="chart-type"]')[1] as HTMLInputElement).click();
    fixture.detectChanges();

    expect(el.querySelector('h2')?.textContent).toContain('Entradas por categoria');
    expect(el.querySelectorAll('.bar-row').length).toBe(1);
    expect(el.querySelector('.bar-row')!.textContent).toContain('Salário');
  });

  it('a tabela mostra os mesmos dados e o total', async () => {
    await render();

    (el.querySelector('.chart-toggle') as HTMLButtonElement).click();
    fixture.detectChanges();

    expect(el.querySelector('.bars')).toBeNull();
    const cells = Array.from(el.querySelectorAll('tbody tr')).map(tr => tr.textContent);
    expect(cells[0]).toContain('Alimentação');
    expect(cells[0]).toContain('75%');
    expect(el.querySelector('tfoot')!.textContent).toMatch(/R\$\s?400,00/);
    expect(el.querySelector('.chart-toggle')!.getAttribute('aria-pressed')).toBe('true');
  });

  it('sem dados no período, avisa em vez de desenhar um gráfico vazio', async () => {
    await render([]);

    expect(el.querySelector('.chart-empty')?.textContent).toContain('Nenhuma saída neste período');
    expect(el.querySelector('.chart-toggle')).toBeNull();
  });
});
