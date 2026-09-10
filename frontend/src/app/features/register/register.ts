import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { messageOf } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { NotificationService } from '../../core/notification.service';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.html',
  styleUrl: '../auth.css',
})
export class RegisterPage {

  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly notifications = inject(NotificationService);

  private readonly errorSummary = viewChild<ElementRef<HTMLElement>>('errorSummary');

  readonly submitting = signal(false);
  readonly serverError = signal<string | null>(null);
  readonly submitted = signal(false);

  readonly form = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(20)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(6)]],
  });

  private readonly messages: Record<string, { label: string; required: string; invalid: string }> = {
    name: {
      label: 'Nome',
      required: 'Informe seu nome.',
      invalid: 'O nome deve ter no máximo 20 caracteres.',
    },
    email: {
      label: 'E-mail',
      required: 'Informe seu e-mail.',
      invalid: 'Informe um e-mail válido, como nome@exemplo.com.',
    },
    password: {
      label: 'Senha',
      required: 'Crie uma senha.',
      invalid: 'A senha deve ter ao menos 6 caracteres.',
    },
  };

  messageFor(control: 'name' | 'email' | 'password'): string {
    const config = this.messages[control];
    return this.form.controls[control].hasError('required') ? config.required : config.invalid;
  }

  showError(control: 'name' | 'email' | 'password'): boolean {
    return this.submitted() && this.form.controls[control].invalid;
  }

  errorList(): { id: string; label: string; message: string }[] {
    if (!this.submitted()) return [];
    return (['name', 'email', 'password'] as const)
      .filter(c => this.form.controls[c].invalid)
      .map(c => ({ id: c, label: this.messages[c].label, message: this.messageFor(c) }));
  }

  submit() {
    this.submitted.set(true);
    this.serverError.set(null);

    if (this.form.invalid) {
      this.focusErrorSummary();
      return;
    }

    const { name, email, password } = this.form.getRawValue();
    this.submitting.set(true);

    this.auth.register(email, name, password).subscribe({
      next: () => {
        // Cadastro feito: ja entra, para o usuario nao digitar tudo de novo.
        this.auth.login(email, password).subscribe({
          next: () => {
            this.notifications.success('Conta criada. Boas-vindas!');
            this.router.navigateByUrl('/painel');
          },
          error: () => {
            this.submitting.set(false);
            this.notifications.info('Conta criada. Faça login para continuar.');
            this.router.navigateByUrl('/entrar');
          },
        });
      },
      error: (err) => {
        this.submitting.set(false);
        this.serverError.set(messageOf(err, 'Não foi possível criar a conta.'));
        this.focusErrorSummary();
      },
    });
  }

  /** O sumario so existe no DOM depois que o Angular renderiza o bloco @if,
   *  o que acontece apos o ciclo atual — dai o setTimeout em vez de microtask. */
  private focusErrorSummary() {
    setTimeout(() => this.errorSummary()?.nativeElement.focus(), 0);
  }

  /** Leva o foco ao campo com erro. Sem o preventDefault, o href de ancora
   *  seria tratado como rota pelo Router e a tela se perderia. */
  focusField(event: Event, id: string) {
    event.preventDefault();
    document.getElementById(id)?.focus();
  }
}
