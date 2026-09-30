# Referência da API

A documentação interativa (Swagger UI) fica em `http://localhost:8080/swagger-ui.html` e o OpenAPI em `http://localhost:8080/v3/api-docs`. Para chamar os endpoints protegidos pela UI: faça login em `POST /v1/auth/login`, clique em **Authorize** e informe o token retornado. Para desligar a documentação, use `SWAGGER_ENABLED=false`.

As rotas da integração bancária (`/bank/...` e o webhook da Pluggy) estão em [integracao-bancaria.md](integracao-bancaria.md).

## Autenticação

| Endpoint | O que faz |
|---|---|
| `POST /v1/auth/register` | cadastra um usuário |
| `POST /v1/auth/login` | devolve o token JWT de acesso e um refresh token |
| `POST /v1/auth/refresh` | troca um refresh token válido por um par novo; o apresentado é invalidado (rotação de uso único) |
| `POST /v1/auth/logout` | revoga o refresh token informado |

Resposta do login:

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "expiresIn": 86400000,
  "refreshToken": "SpBDPlDaixM8zBfCur4A..."
}
```

O `token` vai no header `Authorization: Bearer <token>`. Quando expira, chame `POST /v1/auth/refresh` com o `refreshToken` para obter um par novo, sem pedir a senha de novo.

## Transações

Todas as rotas são do usuário autenticado: ninguém enxerga nem altera transações de outra pessoa.

| Endpoint | O que faz |
|---|---|
| `POST /transaction` | cria uma transação |
| `GET /transaction` | lista com paginação, ordenação e filtros |
| `GET /transaction/{id}` | busca uma transação |
| `PUT /transaction/{id}` | atualiza uma transação |
| `PATCH /transaction/{id}/category` | troca só a categoria |
| `DELETE /transaction/{id}` | remove uma transação |
| `GET /transaction/summary` | entradas, saídas e saldo |
| `GET /transaction/summary/by-category` | total por categoria |

### Criar e atualizar

O campo opcional `occurredOn` (`AAAA-MM-DD`) informa o dia em que o gasto ou a entrada aconteceu; sem ele vale agora. O dia é lido no fuso `America/Sao_Paulo`: hoje guarda o instante atual, um dia passado guarda o meio-dia desse dia. Data futura ou anterior a 2000 responde 422 (`fieldsError[0].field = occurredOn`).

No `PUT`, `occurredOn` segue a mesma regra; omitido, ou igual ao dia já gravado, a data e a hora ficam como estavam. Numa transação **importada do banco** a data vem do banco: pedir outro dia responde 422.

```json
{
  "description": "Salário mensal",
  "amount": 3500.00,
  "category": "WAGE",
  "type": "CASH_ENTRY",
  "occurredOn": "2026-09-05"
}
```

Resposta:

```json
{
  "id": "cf0d4b6e-69f6-4b28-86d3-4c95c6795ff3",
  "description": "Salário mensal",
  "amount": 3500.00,
  "category": "WAGE",
  "type": "CASH_ENTRY",
  "createdDate": "2026-09-10T16:39:38.108341Z"
}
```

A categoria precisa combinar com o tipo (uma entrada numa categoria de saída responde 422 com o campo).

### Trocar a categoria

```http
PATCH /transaction/{id}/category
```

Corpo: `{ "category": "BILLS", "applyToSimilar": true }`. Serve a qualquer transação do usuário e é o único ajuste que uma transação **importada do banco** aceita: o `PUT` recusa (422) mudar valor, descrição ou tipo dela.

Com `applyToSimilar`, a categoria vale também para as transações importadas do mesmo estabelecimento e para as próximas importações. A resposta traz a transação e `updated`, o total de transações que mudaram.

"Mesmo estabelecimento" são as primeiras quatro palavras da descrição, sem acento, número nem símbolo (`Uber *Viagem 1234` e `UBER *TRIP 9F3K` se aproximam). Cada escolha vira uma regra do usuário (tabela `category_rules`): escolher de novo atualiza a regra, e outro usuário nunca é afetado.

### Resumos

```http
GET /transaction/summary?startDate=2026-09-01&endDate=2026-09-29
```

```json
{
  "cashEntry": 5000.00,
  "expenses": 2300.00,
  "balance": 2700.00
}
```

As datas são opcionais e valem sobre o dia em que o gasto ocorreu, no fuso `America/Sao_Paulo`, com o dia final incluído. Sem datas, o resumo cobre tudo. Data inicial depois da final responde 422.

```http
GET /transaction/summary/by-category?startDate=2026-09-01&endDate=2026-09-29
```

Total por categoria no período, do maior para o menor, com a quantidade de transações de cada uma:

```json
[{ "category": "FOOD", "type": "EXPENSES", "total": 812.40, "count": 12 }]
```

## Usuários

| Endpoint | O que faz |
|---|---|
| `POST /user` | cria um usuário |
| `GET /user/{userId}` | consulta o cadastro |
| `PUT /user/{userId}` | atualiza o cadastro |
| `DELETE /user/{userId}` | apaga a conta; antes, revoga as conexões bancárias na Pluggy |

Cada usuário só acessa o **próprio** cadastro. Um id de outro usuário responde `403`, e o mesmo `403` vale para um id inexistente, assim não dá para descobrir quais ids existem. Perfis `ROLE_ADMIN` acessam qualquer cadastro.
