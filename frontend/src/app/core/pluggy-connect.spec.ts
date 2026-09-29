import { TestBed } from '@angular/core/testing';

import { PLUGGY_SDK_LOADER, PluggyConnectService, PluggyWidgetOptions } from './pluggy-connect';

describe('PluggyConnectService', () => {
  let service: PluggyConnectService;
  let options: PluggyWidgetOptions;
  let initResult: () => Promise<void>;

  beforeEach(() => {
    initResult = () => Promise.resolve();
    class FakeWidget {
      constructor(o: PluggyWidgetOptions) {
        options = o;
      }
      init() {
        return initResult();
      }
    }
    TestBed.configureTestingModule({
      providers: [{ provide: PLUGGY_SDK_LOADER, useValue: async () => FakeWidget }],
    });
    service = TestBed.inject(PluggyConnectService);
  });

  it('abre o widget com o token e o modo sandbox', async () => {
    const p = service.open('token-1');
    await new Promise((r) => setTimeout(r));
    expect(options.connectToken).toBe('token-1');
    expect(options.includeSandbox).toBe(true);
    options.onClose?.();
    await p;
  });

  it('abre em modo de reautorização quando recebe o item', async () => {
    const p = service.open('token-1', 'item-9');
    await new Promise((r) => setTimeout(r));
    expect(options.updateItem).toBe('item-9');
    options.onSuccess({ item: { id: 'item-9' } });
    await p;
  });

  it('não manda updateItem ao criar uma conexão nova', async () => {
    const p = service.open('token-1');
    await new Promise((r) => setTimeout(r));
    expect('updateItem' in options).toBe(false);
    options.onClose?.();
    await p;
  });

  it('devolve o id do item quando a conexao termina', async () => {
    const p = service.open('t');
    await new Promise((r) => setTimeout(r));
    options.onSuccess({ item: { id: 'item-9' } });
    expect(await p).toBe('item-9');
  });

  it('devolve null se a pessoa fecha o widget sem concluir', async () => {
    const p = service.open('t');
    await new Promise((r) => setTimeout(r));
    options.onClose?.();
    expect(await p).toBeNull();
  });

  it('rejeita quando o widget reporta erro', async () => {
    const p = service.open('t');
    await new Promise((r) => setTimeout(r));
    options.onError?.({ message: 'credenciais recusadas' });
    await expect(p).rejects.toThrow('credenciais recusadas');
  });

  it('ignora eventos depois do primeiro (sucesso seguido de fechar)', async () => {
    const p = service.open('t');
    await new Promise((r) => setTimeout(r));
    options.onSuccess({ item: { id: 'item-1' } });
    options.onClose?.();
    expect(await p).toBe('item-1');
  });

  it('rejeita quando o init do widget falha', async () => {
    initResult = () => Promise.reject(new Error('render falhou'));
    await expect(service.open('t')).rejects.toThrow('render falhou');
  });
});

describe('PluggyConnectService sem o SDK', () => {
  it('avisa com mensagem clara quando o pacote nao carrega', async () => {
    TestBed.configureTestingModule({
      providers: [{ provide: PLUGGY_SDK_LOADER, useValue: async () => { throw new Error('chunk'); } }],
    });
    await expect(TestBed.inject(PluggyConnectService).open('t'))
      .rejects.toThrow('Não foi possível carregar o widget da Pluggy.');
  });
});
