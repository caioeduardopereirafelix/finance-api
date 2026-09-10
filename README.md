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

## Como rodar

### Com Docker (recomendado)

Sobe API, PostgreSQL, Prometheus e Grafana de uma vez:

```bash
cp .env.example .env
# gere um segredo e coloque em JWT_SECRET no .env
openssl rand -base64 48

docker compose up --build
```

| Serviço    | URL                                |
| ---------- | ---------------------------------- |
| API        | http://localhost:8080              |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Prometheus | http://localhost:9090              |
| Grafana    | http://localhost:3000              |

A porta `9091` (actuator) **não é publicada** de propósito — ela fica acessível
apenas dentro da rede do compose, para o Prometheus.

### Localmente

Precisa de um PostgreSQL rodando e das variáveis de ambiente configuradas:

```bash
./mvnw spring-boot:run
```

### Testes

```bash
./mvnw verify
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

Cria uma nova transação para o usuário autenticado.

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

Atualiza uma transação existente.

```http
DELETE /transaction/{id}
```

Remove uma transação existente.

```http
GET /transaction/summary
```

Retorna o resumo financeiro do usuário autenticado.

## Exemplo de criação de transação

```json
{
  "description": "Salário mensal",
  "amount": 3500.00,
  "category": "WAGE",
  "type": "CASH_ENTRY"
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

Variáveis de ambiente necessárias:

```bash
# obrigatorias — a aplicacao nao sobe sem elas
DB_URL=jdbc:postgresql://localhost:5432/finance
DB_USER=postgres
DB_PASSWORD=postgres
JWT_SECRET=<64+ caracteres aleatorios>

# opcionais (valores padrao entre parenteses)
JWT_EXPIRATION=86400000              # 24h
REFRESH_TOKEN_EXPIRATION=604800000   # 7 dias
CORS_ALLOWED_ORIGINS=http://localhost:5173
MANAGEMENT_PORT=9091
```

Para gerar um `JWT_SECRET`:

```bash
openssl rand -base64 48
```

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

## Próximas melhorias

* Testes de integração contra PostgreSQL real (Testcontainers), hoje a suíte
  roda em H2
* Relatórios mensais
* Paginação por cursor no extrato
* Front-end

## Licença

Distribuído sob a licença MIT. Veja [LICENSE](LICENSE).

## Autor

Desenvolvido por **Caio Eduardo**.

GitHub: https://github.com/caioeduardopereirafelix
