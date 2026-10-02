import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { messageOf } from '../../core/api-error';
import { BankService } from '../../core/bank.service';
import { BankConnection, MOCK_PROVIDER, PLUGGY_PROVIDER } from '../../core/models';
import { NotificationService } from '../../core/notification.service';
import { PluggyConnectService } from '../../core/pluggy-connect';
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
  private readonly pluggy = inject(PluggyConnectService);

  private readonly disconnectDialog = viewChild<ElementRef<HTMLDialogElement>>('disconnectDialog');
  private readonly heading = viewChild<ElementRef<HTMLElement>>('heading');

  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly connections = signal<BankConnection[]>([]);
  readonly connecting = signal(false);
  readonly syncingId = signal<string | null>(null);
  readonly reauthId = signal<string | null>(null);
  readonly pendingDisconnect = signal<BankConnection | null>(null);
  readonly deleteImported = signal(false);
  readonly disconnecting = signal(false);

  readonly mockProvider = MOCK_PROVIDER;
  readonly pluggyProvider = PLUGGY_PROVIDER;

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

  connect() {
    this.connecting.set(true);

    this.banks.connectToken().subscribe({
      next: ({ token, provider }) => {
        if (provider === MOCK_PROVIDER) {
          this.registerAndSync(`demo-${randomId()}`);
        } else if (provider === PLUGGY_PROVIDER) {
          this.openPluggy(token);
        } else {
          this.connecting.set(false);
          this.notifications.info(
            `O provedor "${provider}" está ativo no servidor, mas o widget dele ainda não foi integrado a esta tela.`);
        }
      },
      error: (err) => {
        this.connecting.set(false);
        this.notifications.error(this.friendly(err, 'Não foi possível iniciar a conexão.'));
      },
    });
  }

  private async openPluggy(token: string) {
    try {
      const itemId = await this.pluggy.open(token);
      if (itemId === null) {
        this.connecting.set(false);
        return;
      }
      this.registerAndSync(itemId);
    } catch (e) {
      this.connecting.set(false);
      this.notifications.error(e instanceof Error ? e.message : 'Não foi possível conectar o banco.');
    }
  }

  private registerAndSync(externalId: string) {
    this.banks.connect(externalId).subscribe({
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

  reauthorize(connection: BankConnection) {
    this.reauthId.set(connection.id);

    this.banks.updateToken(connection.id).subscribe({
      next: ({ token, externalId }) => this.openReauth(connection, token, externalId),
      error: (err) => {
        this.reauthId.set(null);
        this.notifications.error(this.friendly(err, 'Não foi possível iniciar a reautorização.'));
      },
    });
  }

  private async openReauth(connection: BankConnection, token: string, itemId: string) {
    try {
      const result = await this.pluggy.open(token, itemId);
      this.reauthId.set(null);
      if (result !== null) {
        this.sync(connection);
      }
    } catch (e) {
      this.reauthId.set(null);
      this.notifications.error(e instanceof Error ? e.message : 'Não foi possível reautorizar o banco.');
    }
  }

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
        this.load();
      },
    });
  }

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
        queueMicrotask(() => this.heading()?.nativeElement.focus());
      },
      error: (err) => {
        this.disconnecting.set(false);
        this.cancelDisconnect();
        this.notifications.error(this.friendly(err, 'Não foi possível desconectar.'));
      },
    });
  }

  name(connection: BankConnection): string {
    return connection.institutionName ?? 'Banco';
  }

  private imported(count: number): string {
    if (count === 0) return 'Nenhuma transação nova.';
    return count === 1 ? '1 transação nova importada.' : `${count} transações novas importadas.`;
  }

  private friendly(err: unknown, fallback: string): string {
    if (err instanceof HttpErrorResponse && err.status === 503) {
      return 'A integração bancária não está habilitada neste servidor.';
    }
    return messageOf(err, fallback);
  }
}

function randomId(): string {
  return globalThis.crypto?.randomUUID?.()
    ?? `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;
}
