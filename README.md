# Finance API

API REST para controle financeiro pessoal, desenvolvida com Java e Spring Boot.  
O projeto permite cadastrar usuários, autenticar com JWT e gerenciar transações financeiras de entrada e saída, como salários, rendas extras, alimentação, transporte, moradia, saúde, lazer, contas e investimentos.

## Sobre o projeto

A Finance API foi criada com o objetivo de praticar e demonstrar conhecimentos em desenvolvimento backend com Spring Boot, autenticação JWT, Spring Security, persistência com JPA/Hibernate e banco de dados PostgreSQL.

A aplicação permite que cada usuário autenticado tenha acesso apenas às suas próprias transações, garantindo separação dos dados por usuário.

## Funcionalidades

- Cadastro de usuários
- Login com autenticação JWT
- Proteção de rotas com Spring Security
- Criação de transações financeiras
- Listagem das transações do usuário autenticado
- Edição de transações
- Remoção de transações
- Resumo financeiro com:
  - total de entradas
  - total de despesas
  - saldo final
- Validação de campos com Bean Validation
- Validação de categoria conforme o tipo da transação
- Tratamento de exceções personalizado

## Tecnologias utilizadas

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA
- Spring Security
- JWT
- PostgreSQL
- Maven
- Lombok
- MapStruct
- Bean Validation
- Hibernate

## Estrutura do projeto

```text
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
