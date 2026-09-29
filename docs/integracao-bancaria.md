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

**Decisões de importação** (conferidas com a documentação de transações e contas)

- **Pendentes** (`PENDING`) são ignoradas: ainda não afetaram o saldo e podem mudar.
  Compras da fatura aberta e parcelas futuras do cartão ficam `PENDING`. A Pluggy
  mantém o mesmo id quando viram `POSTED`, então entram na sincronização seguinte.
- **Cartão de crédito fica de fora por padrão.** O pagamento da fatura já sai da
  conta corrente; importar as compras também contaria o gasto duas vezes. Com
  `PLUGGY_INCLUDE_CREDIT_CARDS=true` entram as compras do cartão e os estornos e
  cashback (`operationType` `ESTORNO`/`CASHBACK`, que viram entrada). Pagamento de
  fatura e qualquer outro crédito do cartão ficam de fora. Contas `CREDIT` que não
  são cartão (empréstimos) nunca entram.
- **Janela do cartão**: como a fatura aberta fica `PENDING` até fechar, e ao fechar a
  compra volta com a data original, a busca do cartão sempre recua 60 dias. As já
  importadas são descartadas pelo `external_id`.
- **Sinal**: vem do campo `type` (`DEBIT` = saída, `CREDIT` = entrada), não do sinal
  do `amount` (no cartão, positivo é compra e negativo é pagamento).
- **Moeda**: `amount` vem na moeda da transação. Fora de BRL usa-se
  `amountInAccountCurrency`; sem ele a movimentação é ignorada, para não gravar
  10 USD como R$ 10.
- **Data**: `date` é ISO 8601 em UTC, e muitas movimentações vêm só com o dia
  (meia-noite UTC, como no exemplo da documentação). No Brasil isso seria 21h do dia
  anterior. Meia-noite exata vira o início daquele dia em `app.zone`; outros
  horários seguem como vieram. **Confirme com dados reais no sandbox.**
- **Categoria**: o campo `category` exige o plano Pro da Pluggy e vem em português
  (ex.: "Transferência"). O texto passa por `BankCategoryMapper`; o que não for
  reconhecido, ou vier vazio, cai em "Outras despesas/receitas". O mapeamento cobre só
  termos comuns: para melhorar, use a tabela de categorias da Pluggy.
- O id do item é validado como UUID antes de virar parte de uma URL.

**Conferido com a documentação da Pluggy:** `POST /auth`, `POST /connect_token`
(`options.clientUserId`), o widget (`pluggy-connect-sdk`: `onSuccess({ item })`,
`onError({ message })`, `onClose`, `includeSandbox`), o formato de contas e o de
transações e a listagem **v2** de transações. O widget vem do pacote npm, carregado só
quando a pessoa clica em conectar.

**Transações: use a v2.** O `GET /transactions` foi desativado e responde 410. O
`GET /v2/transactions` recebe `accountId` e `dateFrom` e pagina por cursor: cada
resposta traz `results` e `next`, uma query string pronta (`?accountId=...&after=...`)
que é anexada **como veio** ao caminho (o cursor é base64: recodificar mudaria o valor).
`next` nulo é a última página. O cliente também acrescenta `dateFrom` se o cursor não o
trouxer, recusa um `next` que não comece com `?` e não gira em círculo se o cursor não
avançar.

**Ainda não conferido:** a validade da `apiKey` (assumido 2h; o cliente também renova
sozinho se a Pluggy responder 401/403) e o comportamento com dados reais (datas,
categorias, cartões). Note que a categoria vem em inglês em alguns exemplos da
documentação ("Fixed Income Investment") e em português em outros ("Transferência").

**Limitações conhecidas**

- A sincronização é por consulta (agendador ou botão). A Pluggy recomenda webhooks
  (`transactions/created`, `updated`, `deleted`); não estão ligados.
- **Exclusões não são propagadas.** Se a Pluggy apagar uma transação, a nossa continua.
- **Id pode mudar.** Quando data, descrição ou valor mudam demais, a Pluggy apaga a
  transação e cria outra com id novo, e ela entraria duplicada. A documentação sugere
  reconciliar por `providerId` (Open Finance) ou `providerCode`.
- Reconectar um banco cria um item novo, com ids novos: as transações do período
  repetido entram de novo.

**Antes de produção**

- `PLUGGY_INCLUDE_SANDBOX` em `frontend/src/app/core/pluggy-connect.ts` deve ser `false`.
- A conexão só recusa item de outro usuário se a Pluggy devolver o `clientUserId` no
  item (o connect token é criado com o id do nosso usuário). Sem o campo, aceita e
  registra um aviso no log. **Confirme no sandbox que o campo vem preenchido**: procure
  `veio sem clientUserId` nos logs da API.
- Item com `LOGIN_ERROR`/`OUTDATED` (o usuário precisa reautorizar) hoje só falha na
  sincronização; falta um fluxo de "renovar consentimento".

## Sincronização automática

Ligue com `BANK_SYNC_ENABLED=true`. O horário vem de `BANK_SYNC_CRON` (6 campos, começando
pelos segundos; padrão `0 0 * * * *`, de hora em hora). Para testar sem esperar uma hora,
use `BANK_SYNC_CRON=0 * * * * *` (todo minuto) e acompanhe `docker compose logs -f api`:
cada conexão sincronizada gera uma linha `Conexao ... sincronizada: N importadas`.

- O agendador sincroniza **todas** as conexões, inclusive as com erro. Uma falha passageira
  (a Pluggy fora do ar) marca a conexão como `ERROR`; na rodada seguinte ela é tentada de
  novo e, se der certo, volta a `ACTIVE` sozinha. Uma conexão quebrada de vez (o banco pede
  login de novo) falha a cada rodada e aparece como aviso no log, até existir o fluxo de
  reautorização.
- A sincronização só lê o que a Pluggy já buscou no banco. A frequência com que a Pluggy
  atualiza cada banco depende do plano dela.
- Com mais de uma instância da API, cada uma rodaria o agendador.

## Desconectar

Ao desconectar, a API **primeiro revoga a autorização no provedor** (na Pluggy, apaga o item,
`DELETE /items/{id}`) e só depois apaga a conexão daqui:

- Se a revogação falhar, a API responde 502 com o motivo, **nada muda localmente** e a pessoa
  pode tentar de novo.
- Um item que a Pluggy já não conhece (404) conta como sucesso, então repetir é seguro. É o que
  acontece se a exclusão local falhar depois de a remota ter dado certo.
- Se o provedor estiver desligado (por exemplo, `BANK_PROVIDER` mudou), a conexão antiga não pode
  ser removida (503) até ele voltar.
- Apagar a **conta do usuário** ainda não revoga as conexões dele na Pluggy.

## Regras de importação

- **Tipo** vem do sinal do valor: negativo é saída, positivo é entrada. O valor
  é gravado sempre positivo.
- **Categoria** é traduzida por `BankCategoryMapper`. O que não for reconhecido
  vira `OTHER_EXPENSE` ("Outras despesas") ou `OTHER_INCOME` ("Outras receitas"),
  conforme o sinal do valor. Pix sem categoria, por exemplo, cai em "Outras
  receitas".
- **Data**: cada transação tem `occurred_at`, o momento em que o gasto ocorreu.
  Nas importadas vem do banco; nas manuais é o momento do lançamento.
  `created_date` continua existindo, mas só diz quando o registro entrou no
  sistema. Listagem, ordenação, filtro de período e a tela usam `occurred_at`.
- **Sem duplicar**: cada transação importada guarda `external_id`
  (`provedor:id`). Existe um índice único parcial por `(usuário, external_id)`,
  então o próprio banco barra repetição, mesmo se o código falhar.
- **Janela**: a primeira sincronização busca 90 dias; as seguintes começam 7 dias
  antes da última, porque bancos lançam movimentações com atraso.
- **Falha do provedor**: a conexão vira `ERROR` e a API responde 502. O
  agendador registra o erro e segue para as outras conexões.

## Para ligar um provedor de verdade

1. Implemente `BankProvider` (4 métodos) numa classe condicionada por propriedade,
   como o `MockBankProvider` ou o `PluggyConfig`.
2. Defina `BANK_PROVIDER` com o `name()` dela.
3. Guarde a chave da API do provedor em variável de ambiente, nunca no repositório.
4. Se o provedor avisa por **webhook**, crie um endpoint que chame
   `BankSyncService.syncById(...)`. Ele precisa validar a assinatura do webhook.

## Pendências conhecidas

- **Fuso do filtro de período.** "De 01/09 até 28/09" são dias no fuso
  `America/Sao_Paulo` (configurável em `app.zone`). Um usuário em outro fuso vê
  as datas no fuso do navegador dele, mas o filtro continua usando esse.
- **Lançamento manual não tem data própria.** Vale o momento em que foi criado.
  Para lançar um gasto de ontem, o formulário precisaria de um campo de data.
- **Dados de demonstração antigos.** Transações importadas do mock *antes* das
  categorias "Outros" continuam com a categoria antiga. Para refazer: desconecte
  com `?deleteImported=true` e sincronize de novo.
- **Editar/apagar importadas.** Nada impede o usuário de editar o valor de uma
  transação importada; a próxima sincronização não a sobrescreve (o
  `external_id` já existe), então a edição fica, mas vale decidir se isso é o
  desejado.
- **Consentimento e LGPD.** Dados financeiros são dados pessoais. Antes de usar
  com dados reais: texto de consentimento na tela, política de retenção e um
  caminho para o usuário apagar tudo (o `deleteImported=true` cobre a parte das
  transações).
- **Testes contra o provedor real.** Só existem contra o mock.
