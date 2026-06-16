# Arquitetura

```text
Cliente
  |
  | texto ou áudio
  v
Controllers REST
  |
  v
FinancialAssistant
  |-----------------------------|
  | modo demo                   | perfil openai
  v                             v
Interpretador local         TranscriptionModel
                                |
                                v
                            ChatClient
                                |
                                v
                          FinancialTools (@Tool)
                                |
                                v
                            Casos de uso
                                |
                                v
                      TransactionRepository
                                |
                                v
                         Spring Data JPA
                                |
                                v
                           H2 Database

No perfil OpenAI, a resposta final passa pelo TextToSpeechModel e retorna MP3.
Todas as interações são registradas na tabela ai_interactions.
```
