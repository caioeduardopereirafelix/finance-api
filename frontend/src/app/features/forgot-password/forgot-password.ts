import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { messageOf } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';

@Component({
  selector: 'app-forgot-password',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.html',
  styleUrl: '../auth.css',
})
export class ForgotPasswordPage {

  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  private readonly errorSummary = viewChild<ElementRef<HTMLElement>>('errorSummary');

  readonly submitting = signal(false);
  readonly serverError = signal<string | null>(null);
  readonly submitted = signal(false);
  readonly sent = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  emailMessage(): string {
    return this.form.controls.email.hasError('required')
      ? 'Informe seu e-mail.'
      : 'Informe um e-mail válido, como nome@exemplo.com.';
  }

  showEmailError(): boolean {
    return this.submitted() && this.form.controls.email.invalid;
  }

  submit() {
    this.submitted.set(true);
    this.serverError.set(null);

    if (this.form.invalid) {
      this.focusErrorSummary();
      return;
    }

    this.submitting.set(true);

    this.auth.forgotPassword(this.form.getRawValue().email).subscribe({
      next: () => {
        this.submitting.set(false);
        this.sent.set(true);
      },
      error: (err) => {
        this.submitting.set(false);
        this.serverError.set(messageOf(err, 'Não foi possível enviar o pedido.'));
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
