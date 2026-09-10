import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { throwError } from 'rxjs';
import { catchError, switchMap } from 'rxjs/operators';

import { AuthService } from './auth.service';

/** Endpoints que nao levam token e nunca disparam renovacao. */
const PUBLIC_PATHS = ['/v1/auth/login', '/v1/auth/register', '/v1/auth/refresh', '/v1/auth/logout'];

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);

  if (PUBLIC_PATHS.some(path => req.url.includes(path))) {
    return next(req);
  }

  const withToken = (token: string) =>
    req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });

  const token = auth.accessToken();
  const request = token ? withToken(token) : req;

  return next(request).pipe(
    catchError((error: HttpErrorResponse) => {
      // Access token expirado: renova uma vez e repete a requisicao original.
      if (error.status === 401 && auth.refreshToken()) {
        return auth.refresh().pipe(
          switchMap(res => next(withToken(res.token))),
          catchError(refreshError => {
            auth.forceLogout();
            return throwError(() => refreshError);
          }),
        );
      }
      return throwError(() => error);
    }),
  );
};
