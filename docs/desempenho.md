# Desempenho

Números medidos, não estimados. Tudo abaixo pode ser reproduzido com os scripts de [`perf/`](../perf).

## Como foi medido

| Item | Valor |
|---|---|
| Máquina | container com 4 vCPUs e 16 GB de RAM; **a carga, a API e o banco dividem a mesma máquina** |
| Banco | PostgreSQL 16.13, configuração padrão (`shared_buffers` 128 MB) |
| API | JAR de 62 MB, `java -Xms512m -Xmx512m`, pool de conexões padrão |
| Dados | **1,05 milhão de transações** na tabela (20 usuários); o usuário medido tem **100 mil**, espalhadas por 3 anos |
| Carga | cliente próprio em Node (`perf/load.mjs`), cada requisição medida com `performance.now()`; 15 s por cenário (10 s com 1 conexão), sem nenhum erro em nenhuma rodada |

Como a carga roda na mesma máquina da API e do banco, os números são **mais pessimistas** do que num servidor com o banco separado.

## Resultado por operação

| Operação | p50 · 1 conexão | p99 · 1 conexão | req/s · 16 conexões | p99 · 16 conexões |
|---|---:|---:|---:|---:|
| Criar transação | 3,8 ms | 7,7 ms | ~1.400 | ~28 ms |
| Resumo do mês | 6,2 ms | 10,7 ms | ~890 | ~42 ms |
| Listar com filtro (30 dias + categoria) | 6,2 ms | 11,6 ms | ~990 | ~39 ms |
| Listar transações (página 1) | 21,9 ms | 34,7 ms | ~185 | ~150 ms |
| Resumo de tudo (100 mil linhas) | 36,5 ms | 64,4 ms | ~97 | ~281 ms |
| Total por categoria (tudo) | 42,8 ms | 84,9 ms | ~85 | ~309 ms |
| Listar transações (página 5000, offset de 50 mil) | 57,4 ms | 97,6 ms | ~80 | ~350 ms |
| Login (bcrypt) | 85,4 ms | 114,6 ms | ~46 | ~108 ms |

Os valores de 16 conexões são a média de duas rodadas seguidas (a diferença entre elas ficou abaixo de 10%).

**Como ler:**

- As telas do dia a dia consultam **períodos** (o painel abre no mês atual). Esses casos ficam em **6 ms** de p50 e perto de **900 a 1.000 requisições por segundo**, mesmo com 100 mil transações do usuário.
- Consultar **tudo** (100 mil linhas) custa dezenas de milissegundos porque agrega no banco; o preço é proporcional ao volume, como esperado.
- **Login lento é proposital**: o bcrypt custa ~85 ms por tentativa para encarecer ataques de força bruta.
- A **primeira página da listagem** custa 22 ms; a maior parte é a contagem total que a paginação faz (veja abaixo), já que buscar os dados leva 0,1 ms.
- **Páginas muito fundas** (offset de 50 mil) são mais caras, o custo conhecido de paginação por offset. O próximo passo natural é paginação por cursor.

## O que o banco faz (EXPLAIN ANALYZE)

Com 1,05 milhão de linhas na tabela e 100 mil do usuário medido (três execuções de cada consulta):

| Consulta | Plano | Tempo no banco |
|---|---|---:|
| Dados da página 1 (`ORDER BY occurred_at DESC LIMIT 10`) | `Index Scan` em `idx_transactions_user_occurred_at`: o índice já entrega na ordem, sem ordenar | **0,07 a 0,10 ms** |
| Resumo do mês (2.668 linhas) | `Bitmap Index Scan` + `Bitmap Heap Scan` no mesmo índice | 3 a 4 ms |
| Contagem total da paginação | `Index Only Scan` no mesmo índice, sem acessar a tabela | 13 a 16 ms |
| Resumo de tudo (`SUM ... GROUP BY type`, 100 mil linhas) | `Parallel Bitmap Heap Scan` com 2 workers | 23 a 26 ms |
| Total por categoria (tudo) | idem | ~30 ms |

O índice `(user_id, occurred_at DESC)` é o que sustenta a listagem e os filtros por período. A contagem total é o que pesa na primeira página: cerca de 14 ms dos 22 ms medidos de ponta a ponta.

### Agregar no banco vs. trazer as linhas

O resumo financeiro faz `SUM`/`GROUP BY` no PostgreSQL em vez de carregar as transações do usuário na memória da aplicação. Medido direto no banco (sem contar o mapeamento de entidades, que só piora o segundo caso):

| | Tempo | Dados que trafegam |
|---|---:|---:|
| `SELECT *` das 100 mil linhas do usuário | 420 a 520 ms | ~13 MB |
| `SUM ... GROUP BY type` | 70 a 100 ms | 2 linhas |

## Outros números

| Item | Valor |
|---|---|
| Subida da API (com Flyway, 7 migrations) | ~11 s |
| Memória residente da API (heap de 512 MB) | ~0,6 GB parada, ~0,8 GB depois da carga |
| Front, carga inicial (produção) | 278 kB brutos, **79 kB transferidos** (gzip); cada tela é carregada sob demanda (painel 6,5 kB, transações 7,1 kB) |

## Reproduzir

Precisa de Node 18 ou mais novo e de um PostgreSQL vazio com a API no ar.

```bash
node perf/criar-usuarios.mjs
```

Carga de 1,05 milhão de transações (leva cerca de 20 s):

```bash
docker compose exec -T postgres psql -U postgres -d finance < perf/seed.sql
```

No PowerShell:

```powershell
Get-Content perf/seed.sql | docker compose exec -T postgres psql -U postgres -d finance
```

Medição:

```bash
node perf/load.mjs
CONNECTIONS=1 DURATION=10 node perf/load.mjs
```

Variáveis: `API_URL` (padrão `http://localhost:8080`), `CONNECTIONS`, `DURATION` (segundos), `WARMUP=0` para pular o aquecimento.

O cenário "Criar transação" grava linhas com a descrição `Carga`. Para voltar ao estado inicial antes de medir de novo:

```sql
DELETE FROM transactions WHERE description = 'Carga';
VACUUM ANALYZE transactions;
```

Os resultados variam com a máquina. Compare sempre duas rodadas no mesmo ambiente, e não o valor absoluto.
