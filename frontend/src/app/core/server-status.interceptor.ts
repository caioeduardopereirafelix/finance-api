import { HttpErrorResponse, HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { finalize, tap } from 'rxjs/operators';

import { ServerStatusService } from './server-status.service';

export const serverStatusInterceptor: HttpInterceptorFn = (req, next) => {
  const status = inject(ServerStatusService);
  const timer = status.watch();

  return next(req).pipe(
    tap({
      next: event => {
        if (event instanceof HttpResponse) {
          status.markAwake();
        }
      },
      error: error => {
        if (error instanceof HttpErrorResponse && error.status > 0 && error.status < 500) {
          status.markAwake();
        }
      },
    }),
    finalize(() => status.cancel(timer)),
  );
};
