import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { messageOf } from '../../core/api-error';
import { BankService } from '../../core/bank.service';
import { BankConnection, MOCK_PROVIDER } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { BrDateTimePipe } from '../../shared/datetime.pipe';

@Component({
  selector: 'app-banks',
  imports: [FormsModule, RouterLink, BrDateTimePipe],
  templateUrl: './banks.html',
  styleUrl: './banks.css',
})
export class BanksPage {

  private readonly banks = inject(BankService);
  private readonly notifications = inject(NotificationService);

  private readonly disconnectDialog = viewChild<ElementRef<HTMLDialogElement>>('disconnectDialog');
  private readonly heading = viewChild<ElementRef<HTMLElement>>('heading');

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly connections = signal<BankConnection[]>([]);
  readonly connecting = signal(false);
  readonly syncingId = signal<string | null>(null);
  readonly pendingDisconnect = signal<BankConnection | null>(null);
  readonly deleteImported = signal(false);
  readonly disconnecting = signal(false);

  readonly mockProvider = MOCK_PROVIDER;

  constructor() {
    this.load();
  }

  load() {
    this.loading.set(true);
    this.error.set(null);

    this.banks.list().subscribe({
      next: (list) => {
        this.connections.set(list);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(this.friendly(err, 'Não foi possível carregar suas conexões.'));
        this.loading.set(false);
      },
    });
  }

  // ---------- conectar ----------

  connect() {
    this.connecting.set(true);

    this.banks.connectToken().subscribe({
      next: ({ provider }) => {
        if (provider !== MOCK_PROVIDER) {
          // Aqui entraria o widget do provedor real, aberto com o token.
          this.connecting.set(false);
          this.notifications.info(
            `O provedor "${provider}" está ativo no servidor, mas o widget dele ainda não foi integrado a esta tela.`);
          return;
        }
        this.connectDemo();
      },
      error: (err) => {
        this.connecting.set(false);
        this.notifications.error(this.friendly(err, 'Não foi possível iniciar a conexão.'));
      },
    });
  }

  /** O provedor de demonstracao nao tem widget: registra uma conexao e ja importa. */
  private connectDemo() {
    this.banks.connect(`demo-${randomId()}`).subscribe({
      next: (connection) => {
        this.banks.sync(connection.id).subscribe({
          next: (result) => {
            this.connecting.set(false);
            this.notifications.success(
              `${connection.institutionName ?? 'Banco'} conectado. ${this.imported(result.imported)}`);
            this.load();
          },
          error: (err) => {
            this.connecting.set(false);
            this.notifications.error(
              `Banco conectado, mas a primeira importação falhou: ${this.friendly(err, 'tente sincronizar de novo.')}`);
            this.load();
          },
        });
      },
      error: (err) => {
        this.connecting.set(false);
        this.notifications.error(this.friendly(err, 'Não foi possível conectar o banco.'));
      },
    });
  }

  // ---------- sincronizar ----------

  sync(connection: BankConnection) {
    this.syncingId.set(connection.id);

    this.banks.sync(connection.id).subscribe({
      next: (result) => {
        this.syncingId.set(null);
        this.notifications.success(this.imported(result.imported));
        this.load();
      },
      error: (err) => {
        this.syncingId.set(null);
        this.notifications.error(this.friendly(err, 'Não foi possível sincronizar.'));
        this.load();   // o backend marca a conexao como "com erro"
      },
    });
  }

  // ---------- desconectar ----------

  askDisconnect(connection: BankConnection) {
    this.pendingDisconnect.set(connection);
    this.deleteImported.set(false);
    this.disconnectDialog()?.nativeElement.showModal();
  }

  cancelDisconnect() {
    this.disconnectDialog()?.nativeElement.close();
  }

  confirmDisconnect() {
    const target = this.pendingDisconnect();
    if (!target) return;

    this.disconnecting.set(true);
    this.banks.disconnect(target.id, this.deleteImported()).subscribe({
      next: () => {
        this.disconnecting.set(false);
        this.cancelDisconnect();
        this.notifications.success(this.deleteImported()
          ? `${this.name(target)} desconectado e as transações importadas foram apagadas.`
          : `${this.name(target)} desconectado. As transações importadas continuam no seu extrato.`);
        this.load();
        // O botao "Desconectar" some da tela junto com a conexao; sem isso o foco
        // cairia no <body> e quem usa teclado ou leitor de tela perderia o lugar.
        queueMicrotask(() => this.heading()?.nativeElement.focus());
      },
      error: (err) => {
        this.disconnecting.set(false);
        this.cancelDisconnect();
        this.notifications.error(this.friendly(err, 'Não foi possível desconectar.'));
      },
    });
  }

  // ---------- apresentacao ----------

  name(connection: BankConnection): string {
    return connection.institutionName ?? 'Banco';
  }

  private imported(count: number): string {
    if (count === 0) return 'Nenhuma transação nova.';
    return count === 1 ? '1 transação nova importada.' : `${count} transações novas importadas.`;
  }

  /** 503 aqui significa "nenhum provedor habilitado", que merece uma frase propria. */
  private friendly(err: unknown, fallback: string): string {
    if (err instanceof HttpErrorResponse && err.status === 503) {
      return 'A integração bancária não está habilitada neste servidor.';
    }
    return messageOf(err, fallback);
  }
}

/** crypto.randomUUID so existe em contexto seguro (https ou localhost). */
function randomId(): string {
  return globalThis.crypto?.randomUUID?.()
    ?? `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}
