import { Location } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { messageOf } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { NotificationService } from '../../core/notification.service';
import { tokenFromFragment } from './token';

type Control = 'password' | 'confirm';

@Component({
  selector: 'app-reset-password',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.html',
  styleUrl: '../auth.css',
})
export class ResetPasswordPage {

  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly location = inject(Location);
  private readonly notifications = inject(NotificationService);

  private readonly errorSummary = viewChild<ElementRef<HTMLElement>>('errorSummary');
  private readonly token = tokenFromFragment(this.route.snapshot.fragment);

  readonly linkInvalid = signal(this.token === null);
  readonly submitting = signal(false);
  readonly serverError = signal<string | null>(null);
  readonly submitted = signal(false);

  readonly form = this.fb.nonNullable.group({
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirm: ['', [Validators.required]],
  });

  private readonly labels: Record<Control, string> = {
    password: 'Nova senha',
    confirm: 'Repita a nova senha',
  };

  constructor() {
    if (this.token !== null) {
      this.location.replaceState('/redefinir-senha');
    }
  }

  messageFor(control: Control): string {
    const field = this.form.controls[control];
    if (field.hasError('required')) {
      return control === 'password' ? 'Crie uma nova senha.' : 'Repita a nova senha.';
    }
    if (control === 'password') {
      return 'A senha deve ter ao menos 8 caracteres.';
    }
    return 'As senhas não conferem.';
  }

  invalid(control: Control): boolean {
    const field = this.form.controls[control];
    if (control === 'confirm' && field.valid) {
      return this.form.controls.password.value !== field.value;
    }
    return field.invalid;
  }

  showError(control: Control): boolean {
    return this.submitted() && this.invalid(control);
  }

  errorList(): { id: string; label: string; message: string }[] {
    if (!this.submitted()) return [];
    return (['password', 'confirm'] as const)
      .filter(c => this.invalid(c))
      .map(c => ({ id: c, label: this.labels[c], message: this.messageFor(c) }));
  }

  submit() {
    this.submitted.set(true);
    this.serverError.set(null);

    if (this.token === null || this.errorList().length) {
      this.focusErrorSummary();
      return;
    }

    this.submitting.set(true);

    this.auth.resetPassword(this.token, this.form.controls.password.value).subscribe({
      next: () => {
        this.notifications.success('Senha alterada. Entre com a nova senha.');
        this.auth.forceLogout();
      },
      error: (err) => {
        this.submitting.set(false);
        if (err instanceof HttpErrorResponse && err.status === 400) {
          this.linkInvalid.set(true);
          return;
        }
        this.serverError.set(messageOf(err, 'Não foi possível redefinir a senha.'));
        this.focusErrorSummary();
      },
    });
  }

  private focusErrorSummary() {
    setTimeout(() => this.errorSummary()?.nativeElement.focus(), 0);
  }

  focusField(event: Event, id: string) {
    event.preventDefault();
    document.getElementById(id)?.focus();
  }
}
