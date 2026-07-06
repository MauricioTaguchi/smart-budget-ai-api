API inteligente de orçamento financeiro desenvolvida com Java 17, Spring Boot e Spring AI.
A aplicação interpreta comandos em linguagem natural, registra transações financeiras, consulta saldo, lista movimentações, remove registros e pode responder em texto ou áudio.

Este projeto demonstra o uso de IA aplicada em uma API real, combinando backend Java, persistência de dados, validação, arquitetura em camadas, Tool Calling e integração com recursos de voz.

Objetivo do projeto

O objetivo do Smart Budget AI API é simular um assistente financeiro capaz de entender comandos como:

“Registre uma despesa de R$ 89,90 com supermercado na categoria alimentação.”

A partir desse comando, a aplicação interpreta a intenção do usuário, executa a operação financeira correspondente, salva os dados no banco e retorna uma resposta estruturada.

Funcionalidades
Processamento de comandos financeiros em texto
Processamento de comandos por áudio
Registro de receitas e despesas
Consulta de saldo
Listagem de transações
Exclusão de transações
Resumo financeiro com receitas, despesas e saldo
Auditoria das interações com IA
Validação de campos obrigatórios
Tratamento global de erros
Perfil demo para testes sem chave de IA
Testes unitários
Tecnologias utilizadas
Java 17
Spring Boot
Spring AI
Spring Web
Spring Data JPA
Bean Validation
OpenAI API
H2 Database
Maven
JUnit 5
Arquitetura do projeto
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

A estrutura separa responsabilidades em camadas, facilitando manutenção, testes e evolução do projeto.

Como executar o projeto
Executar em modo demo, sem chave de IA
.\mvnw.cmd spring-boot:run

A API ficará disponível em:

http://localhost:8080
Executar com OpenAI

Configure a variável de ambiente:

set OPENAI_API_KEY=sua_chave_aqui
set SPRING_PROFILES_ACTIVE=openai
.\mvnw.cmd spring-boot:run
Exemplos de uso
Registrar uma despesa
curl -X POST http://localhost:8080/api/assistant/text ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"Registre uma despesa de 89,90 com supermercado na categoria alimentação\"}"
Registrar uma receita
curl -X POST http://localhost:8080/api/assistant/text ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"Recebi uma receita de 3500 de salário na categoria trabalho\"}"
Consultar saldo
curl -X POST http://localhost:8080/api/assistant/text ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"Qual é o meu saldo atual?\"}"
Enviar áudio
curl -X POST http://localhost:8080/api/assistant/voice ^
  -F "audio=@comando.mp3" ^
  --output resposta.mp3
Endpoints principais
Método	Endpoint	Descrição
POST	/api/assistant/text	Processa comando em texto
POST	/api/assistant/voice	Processa áudio e retorna MP3
POST	/api/transactions	Cria uma transação
GET	/api/transactions	Lista transações
GET	/api/transactions/summary	Consulta resumo financeiro
DELETE	/api/transactions/{id}	Exclui uma transação
GET	/api/audits	Consulta auditoria
GET	/actuator/health	Verifica status da aplicação
Banco de dados H2

Acesse:

http://localhost:8080/h2-console

Configuração:

JDBC URL: jdbc:h2:file:./data/smartbudget
User Name: sa
Password:
Testes

Para executar os testes:

.\mvnw.cmd clean test

Os testes utilizam perfil específico, banco H2 em memória e modelos externos desativados.

O que este projeto demonstra
Desenvolvimento de APIs REST com Java e Spring Boot
Integração de IA generativa com backend
Uso de Tool Calling para executar ações reais
Processamento de texto e áudio
Persistência com JPA e H2
Validação de dados
Arquitetura em camadas
Testes unitários
Organização de projeto para portfólio profissional
Melhorias futuras
Adicionar autenticação com JWT
Criar documentação com Swagger/OpenAPI
Adicionar integração com banco PostgreSQL
Criar dashboard financeiro
Implementar categorias personalizadas
Criar deploy em nuvem
Autor

Maurício Ryo Toita Taguchi
GitHub: MauricioTaguchi
