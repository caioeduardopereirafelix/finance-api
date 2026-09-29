import { Injectable, InjectionToken, inject } from '@angular/core';

export const PLUGGY_INCLUDE_SANDBOX = true;

export interface PluggyWidgetOptions {
  connectToken: string;
  includeSandbox?: boolean;
  onSuccess: (data: { item: { id: string } }) => void | Promise<void>;
  onError?: (error: { message: string }) => void | Promise<void>;
  onClose?: () => void | Promise<void>;
}

export interface PluggyWidget {
  init(): Promise<void>;
}

export type PluggyWidgetFactory = new (options: PluggyWidgetOptions) => PluggyWidget;

export const PLUGGY_SDK_LOADER = new InjectionToken<() => Promise<PluggyWidgetFactory>>('PLUGGY_SDK_LOADER', {
  providedIn: 'root',
  factory: () => async () => {
    const sdk = await import('pluggy-connect-sdk');
    // O build ESM do pacote so tem o export nomeado; o exemplo da documentacao
    // usa `default`, que existe no build CommonJS. Aceita os dois.
    return (sdk.PluggyConnect ?? (sdk as { default?: unknown }).default) as unknown as PluggyWidgetFactory;
  },
});

@Injectable({ providedIn: 'root' })
export class PluggyConnectService {

  private readonly loadSdk = inject(PLUGGY_SDK_LOADER);

  async open(connectToken: string): Promise<string | null> {
    let Widget: PluggyWidgetFactory;
    try {
      Widget = await this.loadSdk();
    } catch {
      throw new Error('Não foi possível carregar o widget da Pluggy.');
    }

    return new Promise<string | null>((resolve, reject) => {
      let finished = false;
      const done = (fn: () => void) => {
        if (!finished) {
          finished = true;
          fn();
        }
      };

      const widget = new Widget({
        connectToken,
        includeSandbox: PLUGGY_INCLUDE_SANDBOX,
        onSuccess: (data) => done(() => resolve(data.item.id)),
        onError: (error) => done(() => reject(new Error(error?.message || 'Falha no widget da Pluggy.'))),
        onClose: () => done(() => resolve(null)),
      });

      widget.init().catch((e) => done(() => reject(e)));
    });
  }
}
