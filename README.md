# Smart Budget AI API

API financeira inteligente desenvolvida em **Java 17**, **Spring Boot** e
**Spring AI**. A aplicação recebe comandos em linguagem natural, identifica
a intenção da pessoa usuária, executa operações financeiras reais e, no
perfil OpenAI, aceita áudio e devolve uma resposta em MP3.

## Objetivo do projeto

O fluxo principal é:

1. Receber um comando em texto ou áudio.
2. Transcrever o áudio para texto.
3. Interpretar a intenção com um modelo de IA.
4. Acionar casos de uso reais por meio de Tool Calling.
5. Criar, listar, excluir ou resumir transações.
6. Persistir as informações no banco.
7. Registrar uma auditoria da interação.
8. Gerar uma resposta em texto ou áudio.

## Melhoria implementada

- auditoria das interações com a IA;
- validação de valores e campos obrigatórios;
- resumo com receitas, despesas e saldo;
- exclusão de transações;
- tratamento global de erros;
- testes unitários;
- perfil `demo` para testes sem chave de IA;
- persistência local com H2 em arquivo.

## Arquitetura

```text
src/main/java/com/mauriciotaguchi/smartbudget
├── domain
│   ├── model
│   └── repository
├── application
│   ├── dto
│   └── usecase
└── infrastructure
    ├── ai
    ├── persistence
    └── web
```

## Tecnologias

- Java 17
- Spring Boot 4
- Spring AI 2
- Spring Web
- Spring Data JPA
- Bean Validation
- OpenAI Chat, Speech-to-Text e Text-to-Speech
- H2 Database
- Maven Wrapper
- JUnit 5

## Executar no Windows sem chave de IA

```bat
.\mvnw.cmd spring-boot:run
```

A API ficará em:

```text
http://localhost:8080
```

### Registrar uma despesa por texto

```bat
curl -X POST http://localhost:8080/api/assistant/text ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"Registre uma despesa de 89,90 com supermercado na categoria alimentação\"}"
```

### Registrar uma receita

```bat
curl -X POST http://localhost:8080/api/assistant/text ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"Recebi uma receita de 3500 de salário na categoria trabalho\"}"
```

### Consultar saldo

```bat
curl -X POST http://localhost:8080/api/assistant/text ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"Qual é o meu saldo atual?\"}"
```

## Executar com OpenAI e áudio

```bat
set OPENAI_API_KEY=sua_chave_aqui
set SPRING_PROFILES_ACTIVE=openai
.\mvnw.cmd spring-boot:run
```

Enviar áudio:

```bat
curl -X POST http://localhost:8080/api/assistant/voice ^
  -F "audio=@comando.mp3" ^
  --output resposta.mp3
```

## Endpoints

| Método | Endpoint | Função |
|---|---|---|
| POST | `/api/assistant/text` | Processar comando em texto |
| POST | `/api/assistant/voice` | Processar áudio e retornar MP3 |
| POST | `/api/transactions` | Criar transação |
| GET | `/api/transactions` | Listar transações |
| GET | `/api/transactions/summary` | Consultar resumo |
| DELETE | `/api/transactions/{id}` | Excluir transação |
| GET | `/api/audits` | Consultar auditoria |
| GET | `/actuator/health` | Verificar a aplicação |

## Banco H2

```text
http://localhost:8080/h2-console
JDBC URL: jdbc:h2:file:./data/smartbudget
User Name: sa
Password:
```

## Testes

```bat
.\mvnw.cmd test
```

## O que aprendi

Aprendi a integrar Spring AI a uma aplicação Java real, utilizando ChatClient,
Tool Calling, transcrição de áudio, síntese de voz, persistência, validação,
arquitetura em camadas, auditoria e testes.

## Autor

**Maurício Ryo Toita Taguchi**

GitHub: `MauricioTaguchi`


## Solução de problemas nos testes

Os testes usam o perfil `test`, com banco H2 em memória e modelos externos
desativados. Execute:

```bat
.\mvnw.cmd clean test
```

Se uma execução anterior estiver aberta, encerre-a com `Ctrl + C` antes de
rodar os testes.
