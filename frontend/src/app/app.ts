import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { Title } from '@angular/platform-browser';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';

import { AuthService } from './core/auth.service';
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
  private readonly router = inject(Router);
  private readonly title = inject(Title);

  private readonly mainRegion = viewChild<ElementRef<HTMLElement>>('mainRegion');

  /** Anuncio de troca de rota: numa SPA o leitor de tela nao percebe a
   *  navegacao sozinho, entao dizemos qual pagina abriu. */
  readonly routeAnnouncement = signal('');

  /** No primeiro carregamento o navegador ja posiciona o foco no topo do
   *  documento e o leitor de tela le o titulo sozinho. Mexer no foco aqui
   *  passaria por cima do link "pular para o conteudo", que e justamente
   *  o primeiro ponto de tabulacao (WCAG 2.4.1). */
  private firstNavigation = true;

  constructor() {
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd))
      .subscribe(() => {
        if (this.firstNavigation) {
          this.firstNavigation = false;
          return;
        }

        const pageTitle = this.title.getTitle().split(' · ')[0];
        this.routeAnnouncement.set(`${pageTitle}. Página carregada.`);

        // Nas navegacoes seguintes o foco continuaria no link clicado, que
        // pode nem existir mais na tela nova. Levamos ele ao inicio do
        // conteudo; o link de pular fica um Shift+Tab atras.
        this.mainRegion()?.nativeElement.focus();
      });
  }

  logout() {
    this.auth.logout();
  }

  toggleTheme() {
    this.theme.toggle();
  }
}
