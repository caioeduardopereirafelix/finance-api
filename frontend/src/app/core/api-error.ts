import { HttpErrorResponse } from '@angular/common/http';

import { ApiError, FieldError } from './models';

/** Traduz a falha HTTP numa frase util para quem esta usando a tela. */
export function messageOf(error: unknown, fallback = 'Não foi possível concluir a operação.'): string {
  if (!(error instanceof HttpErrorResponse)) {
    return fallback;
  }

  if (error.status === 0) {
    return 'Não foi possível falar com o servidor. Verifique sua conexão e tente de novo.';
  }

  const body = error.error as ApiError | undefined;

  if (body?.fieldsError?.length) {
    return body.fieldsError.map(f => f.message).join(' ');
  }

  if (body?.error) {
    return body.error;
  }

  switch (error.status) {
    case 401: return 'E-mail ou senha inválidos.';
    case 403: return 'Você não tem permissão para esta ação.';
    case 404: return 'Registro não encontrado.';
    case 409: return 'Este e-mail já está cadastrado.';
    default: return fallback;
  }
}

/** Erros por campo, para marcar o input correspondente. */
export function fieldErrorsOf(error: unknown): FieldError[] {
  if (error instanceof HttpErrorResponse) {
    return (error.error as ApiError | undefined)?.fieldsError ?? [];
  }
  return [];
}
