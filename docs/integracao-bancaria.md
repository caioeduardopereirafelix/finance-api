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
- Outro provedor: a tela avisa que o widget dele ainda não foi integrado. É aqui que
  entra o SDK do provedor, aberto com o `token`.
- Nenhum provedor habilitado: "A integração bancária não está habilitada neste servidor".

Ao desconectar, o usuário escolhe se apaga também as transações importadas.

## Configuração

| Variável | Padrão | Para quê |
|---|---|---|
| `BANK_MOCK_ENABLED` | `false` | liga o provedor de mentira (só desenvolvimento e demo) |
| `BANK_PROVIDER` | `mock` | nome do provedor usado em **novas** conexões |
| `BANK_SYNC_ENABLED` | `false` | liga a sincronização automática |
| `BANK_SYNC_CRON` | `0 0 * * * *` | horário da sincronização (padrão: de hora em hora) |

Para experimentar agora, ponha no `.env`:

```
BANK_MOCK_ENABLED=true
```

Suba a API, faça login e chame `POST /bank/connections` com qualquer `externalId`
e depois `.../sync`: entram 7 transações de exemplo.

**Nunca ligue o mock em produção:** ele deixaria qualquer usuário "conectar" um
banco falso.

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

1. Implemente `BankProvider` (4 métodos) numa classe `@Component`
   condicionada por propriedade, como o `MockBankProvider`.
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
