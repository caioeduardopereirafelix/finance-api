# Integração bancária (Open Finance)

Esta é a **base** para importar gastos de contas bancárias. Ela ainda **não fala
com nenhum banco de verdade**: falta escrever a implementação de um agregador
(Pluggy, Belvo, ...). Tudo em volta dela já está pronto e testado.

## Como funciona

```
Front                    API                          Provedor (agregador)
  |  POST /bank/connect-token  |                              |
  |--------------------------->|  createConnectToken()        |
  |<---------------------------|----------------------------->|
  |  abre o widget com o token; o usuario autoriza o banco la |
  |  (as credenciais NUNCA passam por esta API)               |
  |  POST /bank/connections {externalId}                      |
  |--------------------------->|  describeConnection()        |
  |  POST /bank/connections/{id}/sync                         |
  |--------------------------->|  fetchTransactions(since)    |
  |<---- {imported, skipped}---|<-----------------------------|
```

## Endpoints

Todos exigem login.

| Método | Rota | O que faz |
|---|---|---|
| POST | `/bank/connect-token` | `{token, provider}`: token do widget e o nome do provedor ativo |
| POST | `/bank/connections` | registra a conexão depois da autorização (`{"externalId": "..."}`) |
| GET | `/bank/connections` | lista as conexões do usuário |
| POST | `/bank/connections/{id}/sync` | importa agora; devolve `{imported, skipped}` |
| DELETE | `/bank/connections/{id}` | desconecta e **mantém** o histórico |
| DELETE | `/bank/connections/{id}?deleteImported=true` | desconecta e apaga o que veio dela |

Sem nenhum provedor habilitado, as rotas de conexão respondem **503**.

## Tela (front)

Em **Bancos** (`/bancos`) o usuário conecta, sincroniza e desconecta. O extrato e o
painel mostram o selo **Banco** nos lançamentos importados.

O botão "Conectar banco" pergunta ao backend qual provedor está ativo:

- `mock`: sem widget; a tela registra uma conexão fictícia e já importa. Cada clique
  cria uma **conta diferente** com as mesmas 7 transações de exemplo.
- `pluggy`: a tela abre o widget da Pluggy com o `token`. O usuário faz login no banco
  dentro do widget; ao concluir, a tela recebe o id do item, registra a conexão e já
  importa.
- Outro provedor: a tela avisa que o widget dele ainda não foi integrado.
- Nenhum provedor habilitado: "A integração bancária não está habilitada neste servidor".

Ao desconectar, o usuário escolhe se apaga também as transações importadas.

## Configuração

| Variável | Padrão | Para quê |
|---|---|---|
| `BANK_MOCK_ENABLED` | `false` | liga o provedor de mentira (só desenvolvimento e demo) |
| `BANK_PROVIDER` | `mock` | nome do provedor usado em **novas** conexões |
| `BANK_SYNC_ENABLED` | `false` | liga a sincronização automática |
| `BANK_SYNC_CRON` | `0 0 * * * *` | horário da sincronização (padrão: de hora em hora) |
| `PLUGGY_CLIENT_ID` | — | liga a Pluggy quando definido (com o secret) |
| `PLUGGY_CLIENT_SECRET` | — | segredo da Pluggy; **só no `.env` ou no ambiente, nunca no Git** |
| `PLUGGY_INCLUDE_CREDIT_CARDS` | `false` | importa também as compras de cartão de crédito |
| `PLUGGY_BASE_URL` | `https://api.pluggy.ai` | só para apontar para um servidor de testes |

Para experimentar agora, ponha no `.env`:

```
BANK_MOCK_ENABLED=true
```

Suba a API, faça login e chame `POST /bank/connections` com qualquer `externalId`
e depois `.../sync`: entram 7 transações de exemplo.

**Nunca ligue o mock em produção:** ele deixaria qualquer usuário "conectar" um
banco falso.

## Pluggy

Ponha no `.env` (que o Git ignora) as credenciais do painel da Pluggy:

```
BANK_PROVIDER=pluggy
PLUGGY_CLIENT_ID=...
PLUGGY_CLIENT_SECRET=...
```

Com o Docker, `docker compose up -d --build api frontend` repassa essas variáveis
para a API. Sem `PLUGGY_CLIENT_ID`, a Pluggy nem é carregada.

**Como funciona** (`bank/pluggy/`):

1. `PluggyClient` troca `clientId`/`clientSecret` por uma `apiKey`, guarda em cache
   (renova antes de vencer, ou uma vez se a Pluggy responder 401/403) e a manda no
   header `X-API-KEY`.
2. `POST /bank/connect-token` pede à Pluggy um *connect token* para o usuário.
3. O widget devolve o id do *item* (um banco autorizado). Esse id é o `externalId`
   da nossa conexão.
4. A sincronização lê as contas do item e as movimentações de cada conta.
