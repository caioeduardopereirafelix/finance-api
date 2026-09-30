# Configuração, testes e Docker

## Configuração local: use um `.env`

Crie um arquivo chamado `.env` na raiz do projeto com o conteúdo abaixo, trocando o `JWT_SECRET` por um valor gerado:

```bash
DB_URL=jdbc:postgresql://localhost:5432/finance
DB_USER=postgres
DB_PASSWORD=postgres
JWT_SECRET=troque-por-um-valor-gerado

JWT_EXPIRATION=86400000
REFRESH_TOKEN_EXPIRATION=604800000
CORS_ALLOWED_ORIGINS=http://localhost:4200
MANAGEMENT_PORT=9091
SWAGGER_ENABLED=true

LOGIN_MAX_ATTEMPTS=5
LOGIN_LOCK_MINUTES=15
JPA_SHOW_SQL=false

BANK_MOCK_ENABLED=false
BANK_PROVIDER=mock
PLUGGY_CLIENT_ID=
PLUGGY_CLIENT_SECRET=
PLUGGY_WEBHOOK_SECRET=
PLUGGY_WEBHOOK_BASE_URL=
```

As quatro primeiras são obrigatórias; a aplicação não sobe sem elas. As demais já trazem os valores padrão mostrados.

| Variável | Para que serve |
|---|---|
| `JWT_SECRET` | assina os tokens; no mínimo 32 bytes. Gere com `openssl rand -base64 48` |
| `JWT_EXPIRATION` / `REFRESH_TOKEN_EXPIRATION` | validade dos tokens, em milissegundos (24 h e 7 dias) |
| `CORS_ALLOWED_ORIGINS` | origens do front liberadas |
| `MANAGEMENT_PORT` | porta do actuator (health e métricas), separada da API |
| `SWAGGER_ENABLED` | liga e desliga a documentação interativa |
| `LOGIN_MAX_ATTEMPTS` / `LOGIN_LOCK_MINUTES` | senhas erradas seguidas para o mesmo e-mail antes de travar o login, e por quantos minutos |
| `JPA_SHOW_SQL` | imprime cada SQL no log (só para depurar) |
| `BANK_*` e `PLUGGY_*` | integração bancária; veja [integracao-bancaria.md](integracao-bancaria.md) |

A aplicação lê esse arquivo automaticamente ao subir, pela IDE ou por `./mvnw spring-boot:run`, então não é preciso configurar variável de ambiente na mão. Variáveis de ambiente, quando existirem, têm precedência sobre o `.env`, que é como o Docker Compose injeta a configuração. O `.env` é ignorado pelo Git.

> Não coloque comentário na mesma linha de um valor: o `#` passaria a fazer parte do valor e a aplicação não sobe.

> **Importante:** o segredo que ficava fixo no `application.yml` no início do projeto está no histórico do Git e deve ser considerado comprometido. Gere um novo em vez de reaproveitá-lo.

## Testes

```bash
./mvnw test
cd frontend && npm ci && npx ng test --watch=false
```

O perfil de teste (`src/test/resources/application-test.yaml`) fixa a configuração de banco, da Pluggy e de login, então os testes **não dependem do seu `.env`**. Sem isso, credenciais reais no `.env` mudariam o resultado da suíte e fariam testes falarem com a Pluggy de verdade. O teste `TestEnvironmentIsolationTest` garante isso.

A maior parte da suíte roda em H2. Os testes de `PostgresIntegrationTest` rodam contra um **PostgreSQL de verdade** (container do Testcontainers, imagem `postgres:16-alpine`), com as migrations do Flyway e `ddl-auto: validate` como em produção: conferem o schema, as restrições, o fuso nos filtros de período, as somas e a exclusão em cascata da conta. Precisam de Docker; sem ele são pulados. Para usar um PostgreSQL que já existe, defina `TEST_PG_URL` (por exemplo `jdbc:postgresql://localhost:5432/finance_test`), `TEST_PG_USER` e `TEST_PG_PASSWORD`; use um banco vazio e descartável, porque o Flyway cria as tabelas nele.

## Banco de dados e migrations

O schema é controlado pelo **Flyway** (`src/main/resources/db/migration`) e o Hibernate roda com `ddl-auto: validate`: a aplicação não cria nem altera tabelas, só valida se o schema bate com as entidades.

### Migration que já rodou nunca se edita

O Flyway guarda o checksum de cada migration no banco e se recusa a subir se o arquivo mudou depois, **mesmo que seja só um comentário ou um espaço no fim da linha** (trocar LF por CRLF não conta). O teste `MigrationImmutabilityTest` trava isso no build: ele guarda o checksum de cada migration e falha se alguma foi alterada ou apagada.

Para mudar o schema, crie sempre uma migration nova (`V8__...`). Antes de commitar, rode `./mvnw test`: o teste diz o checksum da nova migration e a linha exata para acrescentar em `APPLIED`, no próprio teste. Se alguma ferramenta remove comentários dos arquivos do projeto, exclua `src/main/resources/db/migration/` dela.

### Banco criado pela versão antiga

Se você já tem um banco local criado pela versão antiga (que usava `ddl-auto: update`), apague o schema antes de subir a aplicação, para o Flyway assumir o controle a partir da V1:

```sql
DROP SCHEMA public CASCADE; CREATE SCHEMA public;
```

## Docker Compose

O `docker-compose.yml` sobe PostgreSQL, API, front, Prometheus e Grafana. Três variáveis são **obrigatórias** no `.env` da raiz; sem qualquer uma delas o Compose se recusa a subir, mesmo que você suba só alguns serviços (ele lê o arquivo inteiro):

```bash
JWT_SECRET=<gerado>
POSTGRES_PASSWORD=<senha do banco>
GRAFANA_PASSWORD=<senha do Grafana>
```

Não há senha padrão: `postgres/postgres` e `admin/admin` não valem mais. Use valores fortes.

```bash
docker compose up -d --build
```

| Serviço | Endereço |
|---|---|
| Front | http://localhost:4200 |
| API e Swagger | http://localhost:8080 · http://localhost:8080/swagger-ui.html |
| Prometheus | http://localhost:9090 |
| Grafana | http://localhost:3000 |

> **A senha só vale na criação do volume.** O PostgreSQL grava a senha na primeira vez que o volume `postgres_data` é criado, e o Grafana faz o mesmo com `grafana_data`. Mudar a variável depois **não troca** a senha já gravada, e a API deixaria de conectar. Para quem já tem os volumes, há duas saídas:
>
> - manter os valores antigos no `.env` (`POSTGRES_PASSWORD=postgres`), aceitável só em máquina local;
> - trocar a senha dentro do serviço e depois atualizar o `.env`:
>
> ```bash
> docker compose exec postgres psql -U postgres -c "ALTER USER postgres PASSWORD 'nova-senha'"
> docker compose exec grafana grafana cli admin reset-admin-password 'nova-senha'
> ```
>
> Apagar os volumes (`docker compose down -v`) também funciona, mas **apaga todos os dados**.

**Versões fixas.** O Prometheus (`v3.15.0`) e o Grafana (`13.2.3`) têm versão fixa no `docker-compose.yml`; `latest` muda sozinha a cada `docker compose pull`. Para atualizar, troque a versão no arquivo, suba e confira se os painéis e as métricas continuam funcionando. Versões novas costumam migrar o formato dos dados e **não voltam** para uma versão mais velha, então guarde um backup dos volumes antes.

**Saúde da API.** A imagem da API tem um `HEALTHCHECK`: a cada 30 s ela consulta o `/actuator/health` na porta de gerenciamento (`MANAGEMENT_PORT`, padrão 9091). O health inclui a conexão com o banco, então a API fica `unhealthy` se o banco cair e volta a `healthy` sozinha quando ele volta.

```bash
docker compose ps
docker inspect --format '{{json .State.Health}}' finance-api-api-1
```

O `HEALTHCHECK` só informa: o Docker não reinicia um container `unhealthy`.

### Produção: só o front público

O `docker-compose.yml`, sozinho, publica no host o PostgreSQL, a API, o Prometheus e o Grafana, o que serve para desenvolvimento. Em produção, suba com a sobreposição:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

| Serviço | Desenvolvimento | Produção (`docker-compose.prod.yml`) |
|---|---|---|
| Front | `4200` | `FRONTEND_PORT` (padrão `80`), **público** |
| API | `8080` | não publicada (o front encaminha as chamadas pela rede interna) |
| PostgreSQL | `5432` | não publicado |
| Prometheus | `9090` | só em `127.0.0.1:9090` |
| Grafana | `3000` | só em `127.0.0.1:3000` |
| Reinício | manual | `restart: unless-stopped` em todos os serviços |

Precisa do Docker Compose 2.24 ou mais novo (`docker compose version`). Sem essa versão as portas ficariam publicadas, então **confira o resultado** antes de confiar:

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml config | grep -B1 -A3 "ports:"
```

De uma máquina de fora da sua rede, só a porta do front deve responder. Para abrir o Grafana ou o Prometheus no servidor, use um túnel SSH:

```bash
ssh -L 3000:localhost:3000 usuario@seu-servidor
```

O reinício automático fica só na sobreposição de produção, de propósito: na máquina de desenvolvimento, o container da API voltaria sozinho depois de um reinício do Docker e ocuparia a porta 8080.

Esta sobreposição **não faz HTTPS**: o front escuta em HTTP. Coloque na frente um proxy com certificado (o da hospedagem, Caddy, Traefik ou nginx) e exponha só as portas 80 e 443.
