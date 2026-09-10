# Finance — Front-end

Interface web da [Finance API](../README.md), em **Angular 21** (TypeScript).

## Requisitos

* Node 22.12+ (ou 20.19+)
* A API rodando em `http://localhost:8080`

## Como rodar

```bash
npm install
npm start
```

Abre em `http://localhost:4200`.

O `ng serve` usa o `proxy.conf.json` para encaminhar `/v1`, `/transaction` e
`/user` para `http://localhost:8080` — então **não é preciso configurar CORS**
em desenvolvimento.

Para build de produção:

```bash
npm run build      # saída em dist/frontend
npm test           # testes unitários
```

Em produção o front é servido de outra origem, então:

1. configure a URL da API provendo o token `API_BASE_URL` (`src/app/core/api.config.ts`);
2. inclua a origem do front em `CORS_ALLOWED_ORIGINS` na API.

## Telas

| Rota            | O que faz                                                         |
| --------------- | ----------------------------------------------------------------- |
| `/entrar`       | Login                                                             |
| `/criar-conta`  | Cadastro (já entra na conta ao final)                             |
| `/painel`       | Resumo do mês (entradas, saídas, saldo) e últimas transações       |
| `/transacoes`   | Extrato com filtros, paginação, criar, editar e excluir           |

## Como a autenticação funciona

* O login guarda o access token e o refresh token no `localStorage`.
* Um interceptor injeta `Authorization: Bearer …` nas chamadas protegidas.
* Ao receber `401`, o interceptor renova o token **uma vez** e repete a
  requisição original. Se a renovação falhar, a sessão é encerrada.
* Renovações simultâneas compartilham a mesma chamada, para não gastar o
  refresh token (que é de uso único no backend).

> **Nota de segurança:** guardar o token no `localStorage` o deixa exposto a
> XSS. É o caminho possível enquanto a API devolve os tokens no corpo da
> resposta; a alternativa mais segura seria o backend usar cookie `HttpOnly`
> com `SameSite`.

## Acessibilidade

O projeto mira **WCAG 2.2 nível AA**. O que está implementado:

* **Contraste** — paleta calculada, não estimada: texto ≥ 4.5:1, bordas de
  campo e anel de foco ≥ 3:1, nos temas claro e escuro.
* **Teclado** — tudo alcançável por Tab; link "pular para o conteúdo" como
  primeiro ponto de tabulação; foco sempre visível (anel de 3px).
* **Foco em SPA** — ao trocar de rota o foco vai para o conteúdo principal e a
  mudança é anunciada numa região `aria-live` (o leitor de tela não percebe
  navegação de SPA sozinho). No primeiro carregamento o foco não é mexido,
  para não passar por cima do link de pular.
* **Formulários** — todo campo tem `<label>` associado; erros usam
  `aria-invalid` + `aria-describedby`; ao falhar o envio, um sumário de erros
  recebe o foco e lista links para cada campo com problema.
* **Diálogos** — `<dialog>` nativo: foco preso, `Esc` fecha e o foco volta ao
  botão que abriu.
* **Status** — resultados de ação vão para regiões `aria-live` (`polite` para
  sucesso, `assertive` para erro).
* **Não depender de cor** — entradas e saídas são distinguidas por sinal
  (`+`/`−`), etiqueta textual ("Entrada"/"Saída") e texto para leitor de tela,
  além da cor.
* **Reflow** — a 320px a tabela vira lista, sem rolagem horizontal.
* **Preferências do sistema** — tema claro/escuro automático (com alternador
  manual) e `prefers-reduced-motion` respeitado.

### Como isso foi verificado

Auditoria com **axe-core** via Playwright em 11 estados de tela (login, erros
de validação, cadastro, painel vazio e com dados, transações vazia e com
dados, filtros abertos, diálogo, tema escuro e a 320px):
**415 verificações aprovadas, 0 violações.**

Mais 16 verificações manuais de teclado e foco (ordem de tabulação, link de
pular, foco na troca de rota, prisão de foco no diálogo, retorno de foco,
sumário de erros): **todas aprovadas**.

Ferramenta automática não cobre tudo. Continuam recomendados antes de
produção: teste com leitor de tela real (NVDA/VoiceOver) e revisão de
linguagem dos textos.
