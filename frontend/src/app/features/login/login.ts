import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { messageOf } from '../../core/api-error';
import { AuthService } from '../../core/auth.service';
import { NotificationService } from '../../core/notification.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrl: '../auth.css',
})
export class LoginPage {

  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly notifications = inject(NotificationService);

  private readonly errorSummary = viewChild<ElementRef<HTMLElement>>('errorSummary');

  readonly submitting = signal(false);
  readonly serverError = signal<string | null>(null);
  readonly submitted = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(5)]],
  });

  /** Erros mostrados no sumario do topo, cada um com link para o campo. */
  errorList(): { id: string; label: string; message: string }[] {
    if (!this.submitted()) return [];

    const errors: { id: string; label: string; message: string }[] = [];
    const email = this.form.controls.email;
    const password = this.form.controls.password;

    if (email.invalid) {
      errors.push({
        id: 'email',
        label: 'E-mail',
        message: email.hasError('required')
          ? 'Informe seu e-mail.'
          : 'Informe um e-mail válido, como nome@exemplo.com.',
      });
    }
    if (password.invalid) {
      errors.push({
        id: 'password',
        label: 'Senha',
        message: password.hasError('required')
          ? 'Informe sua senha.'
          : 'A senha deve ter ao menos 5 caracteres.',
      });
    }
    return errors;
  }

  showError(control: 'email' | 'password'): boolean {
    return this.submitted() && this.form.controls[control].invalid;
  }

  submit() {
    this.submitted.set(true);
    this.serverError.set(null);

    if (this.form.invalid) {
      // Leva o foco ao sumario: quem usa teclado ou leitor de tela
      // descobre o que falta sem varrer o formulario.
      this.focusErrorSummary();
      return;
    }

    const { email, password } = this.form.getRawValue();
    this.submitting.set(true);

    this.auth.login(email, password).subscribe({
      next: () => {
        this.notifications.success('Bem-vindo de volta!');
        const redirect = this.route.snapshot.queryParamMap.get('redirect');
        this.router.navigateByUrl(redirect ?? '/painel');
      },
      error: (err) => {
        this.submitting.set(false);
        this.serverError.set(messageOf(err, 'Não foi possível entrar.'));
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
