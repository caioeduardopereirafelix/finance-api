import { InjectionToken } from '@angular/core';

/**
 * Base da API.
 *
 * Em desenvolvimento fica vazio: o proxy do `ng serve` (proxy.conf.json)
 * encaminha /v1 e /transaction para http://localhost:8080, entao nao ha CORS.
 * Em producao, informe a URL completa ao prover este token.
 */
export const API_BASE_URL = new InjectionToken<string>('API_BASE_URL', {
  providedIn: 'root',
  factory: () => '',
});
