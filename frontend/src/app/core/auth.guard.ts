import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isLoggedIn()) {
    return true;
  }

  // Guarda o destino para voltar a ele depois do login.
  return router.createUrlTree(['/entrar'], { queryParams: { redirect: state.url } });
};

/** Impede que quem ja esta autenticado veja login/cadastro de novo. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.isLoggedIn() ? router.createUrlTree(['/painel']) : true;
};
