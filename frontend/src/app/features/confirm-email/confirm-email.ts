import { Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { messageOf } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { NotificationService } from '../../core/notification.service';
import { tokenFromFragment } from '../reset-password/token';

type State = 'verifying' | 'done' | 'invalid' | 'failed';

@Component({
  selector: 'app-confirm-email',
  imports: [RouterLink],
  templateUrl: './confirm-email.html',
  styleUrl: '../auth.css',
})
export class ConfirmEmailPage {

  readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);
  private readonly notifications = inject(NotificationService);

  private readonly token = tokenFromFragment(this.route.snapshot.fragment);

  readonly state = signal<State>(this.token === null ? 'invalid' : 'verifying');
  readonly failure = signal<string | null>(null);
  readonly resending = signal(false);

  constructor() {
    if (this.token !== null) {
      this.location.replaceState('/confirmar-email');
      this.verify();
    }
  }

  verify() {
    if (this.token === null) {
      return;
    }
    this.state.set('verifying');
    this.auth.verifyEmail(this.token).subscribe({
      next: () => this.state.set('done'),
      error: (err) => {
        if (err instanceof HttpErrorResponse && err.status === 400) {
          this.state.set('invalid');
          return;
        }
        this.failure.set(messageOf(err, 'Não foi possível confirmar o e-mail.'));
        this.state.set('failed');
      },
    });
  }

  resend() {
    this.resending.set(true);
    this.auth.resendVerification().subscribe({
      next: () => {
        this.resending.set(false);
        this.notifications.success('Enviamos um novo link para o seu e-mail.');
      },
      error: (err) => {
        this.resending.set(false);
        this.notifications.error(messageOf(err, 'Não foi possível reenviar o e-mail.'));
      },
    });
  }
}
