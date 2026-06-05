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
* Proteção de rotas com Spring Security
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
└── service
```

## Principais endpoints

### Autenticação

```http
POST /v1/auth/register
```

Cadastro de novo usuário.

```http
POST /v1/auth/login
```

Autenticação do usuário e geração do token JWT.

### Transações

```http
POST /transaction
```

Cria uma nova transação para o usuário autenticado.

```http
GET /transaction
```

Lista as transações do usuário autenticado.

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
  "expense": 2300.00,
  "balance": 2700.00
}
```

## Status do projeto

Projeto em desenvolvimento.

A API já possui as principais funcionalidades de autenticação, segurança, CRUD de transações, resumo financeiro, validações e persistência com PostgreSQL.

## Próximas melhorias

* Implementar testes automatizados
* Adicionar documentação com Swagger/OpenAPI
* Adicionar relatórios mensais
* Implementar Front-end

## Autor

Desenvolvido por **Caio Eduardo**.

GitHub: https://github.com/caioeduardopereirafelix
