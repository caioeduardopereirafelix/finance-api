# Finance API

API REST para controle financeiro pessoal, desenvolvida com **Java 21** e **Spring Boot**.

O projeto permite que usuários se cadastrem, realizem autenticação com **JWT** e gerenciem suas próprias transações financeiras de entrada e saída, como salários, rendas extras, alimentação, transporte, moradia, saúde, lazer, contas e investimentos.

## Sobre o projeto

A **Finance API** foi criada com o objetivo de praticar e demonstrar conhecimentos em desenvolvimento backend utilizando Spring Boot, Spring Security, autenticação JWT, JPA/Hibernate e PostgreSQL.

A aplicação segue uma arquitetura em camadas, separando responsabilidades entre controllers, services, repositories, DTOs, entidades, mappers e tratamento de exceções.

Um dos principais pontos do projeto é a segurança dos dados: cada usuário autenticado acessa apenas suas próprias transações, garantindo isolamento das informações por usuário.

## Funcionalidades

* Cadastro de usuários
* Login com autenticação JWT
* Refresh token com rotação e revogação (logout)
* Proteção de rotas com Spring Security
* Isolamento de dados por usuário autenticado
* Autorização baseada em roles
* Criação de transações financeiras
* Listagem das transações do usuário autenticado
* Edição de transações
* Remoção de transações
* Resumo financeiro com:

  * total de entradas
  * total de despesas
  * saldo final
* Filtros de transações
* Paginação e ordenação
* Validação de campos com Bean Validation
* Validação de categoria conforme o tipo da transação
* Tratamento de exceções personalizado
* Persistência de dados com PostgreSQL

## Tecnologias utilizadas

* Java 21
* Spring Boot
* Spring Web
* Spring Data JPA
* Spring Security
* JWT
* PostgreSQL
* Maven
* Lombok
* MapStruct
* Bean Validation
* Hibernate
* Flyway
* Docker / Docker Compose
* Swagger / OpenAPI (springdoc)
* Prometheus e Grafana
* JUnit 5, Mockito e Spring Security Test

## Conceitos aplicados

* API REST
* Autenticação stateless com JWT
* Autorização com Spring Security
* Organização em camadas
* DTOs para entrada e saída de dados
* Mapeamento de entidades com JPA
* Relacionamento entre usuários, roles e transações
* Validação de dados
* Tratamento centralizado de erros
* Paginação, ordenação e filtros
* Controle de acesso por usuário autenticado

## Estrutura do projeto

```txt
src/main/java/io/github/caioeduardopereirafelix/financeapi
├── config
├── controller
├── exceptions
├── model
│   ├── dto
│   ├── entity
│   ├── enums
│   └── mapper
├── repository
├── specification
└── service
    └── validator
```
## Documentação da API

Com a aplicação rodando, a documentação interativa fica em:

* Swagger UI: http://localhost:8080/swagger-ui.html
* OpenAPI JSON: http://localhost:8080/v3/api-docs

Para chamar os endpoints protegidos pela UI: faça login em `POST /v1/auth/login`,
clique em **Authorize** e informe o token retornado.

Para desligar a documentação (em produção, por exemplo), use `SWAGGER_ENABLED=false`.

## Principais endpoints

### Autenticação

```http
POST /v1/auth/register
```

Cadastro de novo usuário.

```http
POST /v1/auth/login
```

Autenticação do usuário. Retorna o token JWT de acesso e um refresh token.

```http
POST /v1/auth/refresh
```

Troca um refresh token válido por um novo par de tokens. O refresh token
apresentado é invalidado no processo (rotação de uso único).

```http
POST /v1/auth/logout
```

Revoga o refresh token informado.

### Transações

```http
POST /transaction
```

Cria uma nova transação para o usuário autenticado. O campo opcional `occurredOn` (`AAAA-MM-DD`)
informa o dia em que o gasto ou a entrada aconteceu; sem ele vale agora. O dia é lido no fuso
`America/Sao_Paulo`: hoje guarda o instante atual, um dia passado guarda o meio-dia desse dia.
Data futura ou anterior a 2000 responde 422 (`fieldsError[0].field = occurredOn`).

```http
GET /transaction
```

Lista as transações do usuário autenticado (paginada, com filtros).

```http
GET /transaction/{id}
```

Retorna uma transação específica do usuário autenticado.

```http
PUT /transaction/{id}
```

Atualiza uma transação existente. `occurredOn` segue a regra da criação; omitido, ou igual ao dia
já gravado, a data e a hora ficam como estavam. Numa transação **importada do banco** a data vem do
banco: pedir outro dia responde 422.

```http
PATCH /transaction/{id}/category
```

Troca só a categoria (corpo: `{ "category": "BILLS", "applyToSimilar": true }`). Serve a qualquer
transação do usuário e é o único ajuste que uma transação **importada do banco** aceita: o `PUT`
recusa (422) mudar valor, descrição ou tipo dela. Com `applyToSimilar`, a categoria vale também
para as transações importadas do mesmo estabelecimento e para as próximas importações. A resposta
traz a transação e `updated`, o total de transações que mudaram. Categoria de outro tipo (uma
entrada numa saída) responde 422.

"Mesmo estabelecimento" são as primeiras quatro palavras da descrição, sem acento, número nem
símbolo (`Uber *Viagem 1234` e `UBER *TRIP 9F3K` se aproximam). Cada escolha vira uma regra do
usuário (tabela `category_rules`): escolher de novo atualiza a regra, e outro usuário nunca é
afetado.

```http
DELETE /transaction/{id}
```

Remove uma transação existente.

```http
GET /transaction/summary?startDate=2026-09-01&endDate=2026-09-29
```

Retorna o resumo financeiro (entradas, saídas e saldo) do usuário autenticado. As datas são
opcionais e valem sobre o dia em que o gasto ocorreu, no fuso `America/Sao_Paulo`, com o dia final
incluído. Sem datas, o resumo cobre tudo. Data inicial depois da final responde 422.

```http
GET /transaction/summary/by-category?startDate=2026-09-01&endDate=2026-09-29
```

Retorna o total por categoria no período, do maior para o menor, com a quantidade de
transações de cada uma: `[{ "category": "FOOD", "type": "EXPENSES", "total": 812.40, "count": 12 }]`.

## Exemplo de criação de transação

```json
{
  "description": "Salário mensal",
  "amount": 3500.00,
  "category": "WAGE",
  "type": "CASH_ENTRY",
  "occurredOn": "2026-09-05"
}
```

## Exemplo de resposta do resumo financeiro

```json
{
  "cashEntry": 5000.00,
  "expenses": 2300.00,
  "balance": 2700.00
}
```

## Exemplo de resposta de transação

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

## Exemplo de resposta da autenticação

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "expiresIn": 86400000,
  "refreshToken": "SpBDPlDaixM8zBfCur4A..."
}
```

O `token` é usado no header `Authorization: Bearer <token>`. Quando ele expira,
chame `POST /v1/auth/refresh` com o `refreshToken` para obter um par novo — não
é preciso pedir a senha ao usuário de novo.

## Segurança

* Cada usuário só enxerga e altera o **próprio** cadastro e as **próprias**
  transações. Um `GET`/`PUT`/`DELETE` em `/user/{id}` de outro usuário responde
  `403`, e o mesmo `403` vale para um id inexistente — assim não dá para
  descobrir quais ids existem.
* Perfis `ROLE_ADMIN` têm acesso a qualquer cadastro.
* Refresh tokens são opacos e ficam no banco **apenas como hash SHA-256**. Cada
  um vale para um único uso: ao ser trocado, é revogado.
* O `/actuator` não responde mais na porta pública da API — ele fica numa porta
  separada (`MANAGEMENT_PORT`, padrão `9091`), que **não deve ser exposta fora
  da rede interna**.

## Banco de dados e migrations

O schema é controlado pelo **Flyway** (`src/main/resources/db/migration`) e o Hibernate
roda com `ddl-auto: validate` — ou seja, a aplicação não cria nem altera tabelas,
apenas valida se o schema bate com as entidades.

### Migration que já rodou nunca se edita

O Flyway guarda o checksum de cada migration no banco e se recusa a subir se o arquivo mudou
depois — **mesmo que seja só um comentário ou um espaço no fim da linha** (trocar quebra de
linha, LF por CRLF, não conta). O teste `MigrationImmutabilityTest` trava isso no build: ele guarda o
checksum de cada migration e falha se alguma foi alterada ou apagada.

Para mudar o schema, crie sempre uma migration nova (`V7__...`, `V8__...`). Antes de commitar,
rode `./mvnw test`: o teste diz o checksum da nova migration e a linha exata para acrescentar em
`APPLIED`, no próprio teste. Se alguma ferramenta remove comentários dos arquivos do projeto, exclua
`src/main/resources/db/migration/` dela.

### Configuração local: use um `.env`

Crie um arquivo chamado `.env` na raiz do projeto com o conteúdo abaixo,
trocando o `JWT_SECRET` por um valor gerado:

```bash
# obrigatorias — a aplicacao nao sobe sem elas
DB_URL=jdbc:postgresql://localhost:5432/finance
DB_USER=postgres
DB_PASSWORD=postgres
JWT_SECRET=troque-por-um-valor-gerado

# opcionais — os valores abaixo ja sao os padroes
# expiracao do access token em milissegundos (24h)
JWT_EXPIRATION=86400000
# expiracao do refresh token em milissegundos (7 dias)
REFRESH_TOKEN_EXPIRATION=604800000
CORS_ALLOWED_ORIGINS=http://localhost:4200
MANAGEMENT_PORT=9091
SWAGGER_ENABLED=true

# seguranca (opcional) — os valores abaixo ja sao os padroes
# senhas erradas seguidas para o mesmo e-mail antes de travar o login, e por quantos minutos
LOGIN_MAX_ATTEMPTS=5
LOGIN_LOCK_MINUTES=15
# imprime cada SQL no log (util so para depurar)
JPA_SHOW_SQL=false

# integracao bancaria (opcional) — veja docs/integracao-bancaria.md
BANK_MOCK_ENABLED=false
BANK_PROVIDER=mock
PLUGGY_CLIENT_ID=
PLUGGY_CLIENT_SECRET=
# webhooks da Pluggy (opcional; a API precisa estar acessivel pela internet)
PLUGGY_WEBHOOK_SECRET=
PLUGGY_WEBHOOK_BASE_URL=
```

Para gerar o `JWT_SECRET`:

```bash
openssl rand -base64 48
```

A aplicação lê esse arquivo automaticamente ao subir — pela IDE ou por
`./mvnw spring-boot:run` —, então **não é preciso configurar variável de
ambiente na mão**. Variáveis de ambiente, quando existirem, têm precedência
sobre o `.env`, que é como o Docker Compose injeta a configuração.

O `.env` é ignorado pelo Git, então o segredo não vai para o repositório.

Os testes (`./mvnw test`) **não dependem do seu `.env`**: o perfil de teste
(`src/test/resources/application-test.yaml`) fixa a configuração de banco, da Pluggy e de login. Sem
isso, credenciais reais no `.env` mudariam o resultado da suíte e fariam testes falarem com a
Pluggy de verdade. Um teste (`TestEnvironmentIsolationTest`) garante isso.

A maior parte da suíte roda em H2. Os testes de `PostgresIntegrationTest` rodam contra um
**PostgreSQL de verdade** (container do Testcontainers, imagem `postgres:16-alpine`), com as
migrations do Flyway e `ddl-auto: validate` como em produção: conferem o schema, as restrições, o
fuso nos filtros de período, as somas e a exclusão em cascata da conta. Precisam de Docker; sem ele
são pulados. Para usar um PostgreSQL que já existe, defina `TEST_PG_URL` (por exemplo
`jdbc:postgresql://localhost:5432/finance_test`), `TEST_PG_USER` e `TEST_PG_PASSWORD` — use um banco
vazio e descartável, porque o Flyway cria as tabelas nele.

> Não coloque comentário na mesma linha de um valor: o `#` passaria a fazer
> parte do valor e a aplicação não sobe.

> **Importante:** o segredo que ficava fixo no `application.yml` está no
> histórico do Git e deve ser considerado comprometido. Gere um novo em vez de
> reaproveitá-lo.

> **Atenção:** se você já tem um banco local criado pela versão antiga
> (que usava `ddl-auto: update`), apague o schema antes de subir a aplicação,
> para que o Flyway assuma o controle a partir da V1:
>
> ```sql
> DROP SCHEMA public CASCADE; CREATE SCHEMA public;
> ```

## Status do projeto

Projeto em desenvolvimento.

A API cobre autenticação com JWT e refresh token, isolamento de dados por
usuário, CRUD de transações, resumo financeiro, filtros com paginação,
validações, migrations versionadas com Flyway, documentação OpenAPI,
métricas via Prometheus/Grafana e execução completa via Docker Compose.


## Autor

Desenvolvido por **Caio Eduardo**.

GitHub: https://github.com/caioeduardopereirafelix
