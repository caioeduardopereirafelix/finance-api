import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink],
  template: `
    <div class="page">
      <h1>Página não encontrada</h1>
      <p>O endereço que você abriu não existe ou foi movido.</p>
      <a routerLink="/painel" class="btn btn-primary">Ir para o painel</a>
    </div>
  `,
})
export class NotFoundPage {}
