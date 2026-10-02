import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Transaction } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { TransactionsPage } from './transactions';

const manual: Transaction = {
  id: 'm-1', description: 'Café', amount: 8.5, category: 'FOOD', type: 'EXPENSES',
  occurredAt: '2026-09-21T12:00:00Z', source: 'MANUAL', createdDate: '2026-09-21T12:00:00Z',
};

describe('TransactionsPage (importar extrato dentro de Nova transação)', () => {
  let fixture: ComponentFixture<TransactionsPage>;
  let page: TransactionsPage;
  let http: HttpTestingController;
  let el: HTMLElement;

  const flushList = (content: Transaction[]) => {
    http.expectOne(r => r.url === '/transaction').flush({
      content, totalElements: content.length, totalPages: 1, number: 0, size: 10,
    });
    fixture.detectChanges();
  };

  beforeEach(() => {
    const proto = HTMLDialogElement.prototype as unknown as Record<string, unknown>;
    proto['showModal'] ??= function () { };
    proto['close'] ??= function () { };

    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(TransactionsPage);
    page = fixture.componentInstance;
    el = fixture.nativeElement;
    fixture.detectChanges();
    flushList([manual]);
  });

  afterEach(() => http.verify());

  const openImport = () => {
    page.openCreate();
    page.chooseMode('import');
    fixture.detectChanges();
  };

  const choose = (file: File) => {
    page.onStatementChosen({ target: { files: { item: () => file } } } as unknown as Event);
    fixture.detectChanges();
  };

  const submitButton = () => el.querySelector<HTMLButtonElement>('#form-dialog-titulo ~ form button[type=submit]')!;

  it('Nova transação abre digitando, com a opção de importar um extrato', () => {
    page.openCreate();
    fixture.detectChanges();

    expect(el.querySelector('#form-dialog-titulo')!.textContent).toContain('Nova transação');
    expect(el.querySelector('#t-descricao')).not.toBeNull();
    const options = Array.from(el.querySelectorAll('.mode-choice label')).map(l => l.textContent!.trim());
    expect(options).toEqual(['Digitar uma transação', 'Importar extrato']);
  });

  it('escolher importar troca o formulário pelo envio de arquivo', () => {
    openImport();

    expect(el.querySelector('#form-dialog-titulo')!.textContent).toContain('Importar extrato');
    expect(el.querySelector('#t-descricao')).toBeNull();
    expect(el.querySelector('#statement-file')).not.toBeNull();
    expect(submitButton().disabled).toBe(true);
  });

  it('editar uma transação não oferece importar', () => {
    page.openEdit(manual);
    fixture.detectChanges();

    expect(el.querySelector('.mode-choice')).toBeNull();
    expect(el.querySelector('#form-dialog-titulo')!.textContent).toContain('Editar transação');
  });

  it('envia o arquivo, mostra o resumo, avisa e recarrega a lista', () => {
    openImport();
    choose(new File(['x'], 'extrato.ofx'));
    expect(submitButton().disabled).toBe(false);

    submitButton().click();
    fixture.detectChanges();

    const req = http.expectOne('/transaction/import');
    expect((req.request.body as FormData).get('invertSign')).toBe('false');
    req.flush({ total: 5, imported: 3, skipped: 1, invalid: 1, problems: ['Linha 4: data invalida'] });
    fixture.detectChanges();

    const summary = el.querySelector('.import-result')!.textContent!;
    expect(summary).toContain('novas transações');
    expect(summary).toContain('Linha 4: data invalida');
    expect(TestBed.inject(NotificationService).notices().map(n => n.text)).toContain('3 transações importadas do arquivo.');
    flushList([manual]);
  });

  it('a opção de inverter o sinal vai junto no envio', () => {
    openImport();
    choose(new File(['x'], 'fatura.csv'));
    const checkbox = el.querySelector<HTMLInputElement>('.check-option input')!;
    checkbox.checked = true;
    checkbox.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    submitButton().click();
    const req = http.expectOne('/transaction/import');
    expect((req.request.body as FormData).get('invertSign')).toBe('true');
    req.flush({ total: 1, imported: 1, skipped: 0, invalid: 0, problems: [] });
    flushList([manual]);
  });

  it('erro do servidor aparece no diálogo e o arquivo pode ser trocado', () => {
    openImport();
    choose(new File(['x'], 'lixo.txt'));
    submitButton().click();
    fixture.detectChanges();

    http.expectOne('/transaction/import').flush(
      { status: 422, error: 'Erro Validacacao', fieldsError: [{ field: 'file', message: 'Nao reconhecemos as colunas do CSV.' }] },
      { status: 422, statusText: 'Unprocessable Entity' });
    fixture.detectChanges();

    expect(el.querySelector('.import-error')!.textContent).toContain('Nao reconhecemos as colunas');
    expect(submitButton().disabled).toBe(false);
  });

  it('arquivo acima de 2 MB é barrado antes de enviar', () => {
    openImport();
    choose(new File([new Uint8Array(2 * 1024 * 1024 + 1)], 'grande.csv'));

    expect(el.querySelector('.import-error')!.textContent).toContain('mais de 2 MB');
    expect(submitButton().disabled).toBe(true);
    http.expectNone('/transaction/import');
  });

  it('voltar para digitar limpa o que era do envio', () => {
    openImport();
    choose(new File(['x'], 'extrato.ofx'));

    page.chooseMode('manual');
    fixture.detectChanges();

    expect(el.querySelector('#statement-file')).toBeNull();
    expect(page.statementFile()).toBeNull();
    expect(el.querySelector('#t-descricao')).not.toBeNull();
  });

  it('abrir Nova transação de novo volta ao modo digitar e zera o resultado anterior', () => {
    openImport();
    page.importResult.set({ total: 1, imported: 1, skipped: 0, invalid: 0, problems: [] });

    page.openCreate();
    fixture.detectChanges();

    expect(page.mode()).toBe('manual');
    expect(page.importResult()).toBeNull();
  });
});
