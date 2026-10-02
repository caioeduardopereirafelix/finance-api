import { Component, ElementRef, effect, inject, signal, untracked, viewChild } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';

import { messageOf } from './core/api-error';
import { AuthService } from './core/auth.service';
import { NotificationService } from './core/notification.service';
import { ServerStatusService } from './core/server-status.service';
import { ThemeService } from './shared/theme.service';
import { ToastRegion } from './shared/toast-region';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ToastRegion],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {

  readonly auth = inject(AuthService);
  readonly theme = inject(ThemeService);
  readonly server = inject(ServerStatusService);
  private readonly router = inject(Router);
  private readonly title = inject(Title);
  private readonly notifications = inject(NotificationService);

  private readonly mainRegion = viewChild<ElementRef<HTMLElement>>('mainRegion');

  readonly routeAnnouncement = signal('');
  readonly resending = signal(false);

  private firstNavigation = true;

  constructor() {
    effect(() => {
      if (this.auth.isLoggedIn()) {
        untracked(() => this.auth.loadProfile());
      }
    });

    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(() => {
        if (this.firstNavigation) {
          this.firstNavigation = false;
          return;
        }

        const pageTitle = this.title.getTitle().split(' · ')[0];
        this.routeAnnouncement.set(`${pageTitle}. Página carregada.`);
        this.mainRegion()?.nativeElement.focus();
      });
  }

  resendVerification() {
    this.resending.set(true);
    this.auth.resendVerification().subscribe({
      next: () => {
        this.resending.set(false);
        this.notifications.success('E-mail reenviado. Confira sua caixa de entrada.');
      },
      error: (err) => {
        this.resending.set(false);
        this.notifications.error(messageOf(err, 'Não foi possível reenviar o e-mail.'));
      },
    });
  }

  logout() {
    this.auth.logout();
  }

  toggleTheme() {
    this.theme.toggle();
  }
}
