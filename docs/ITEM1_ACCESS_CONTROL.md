# Item 1 — identidade e isolamento de acesso

Esta entrega limita-se à autenticação individual, administração de credenciais e segregação por proprietário. Nenhuma implantação ou migração de banco operacional deve ser presumida a partir da alteração do código.

## Contrato de identidade

Cada principal tem UUID, `username`, papel `USER` ou `ADMIN`, estado ativo, hash SHA-256 do token e instante de criação. O `username` tem de 3 a 64 caracteres, começa com letra ou número e aceita apenas letras ASCII, números, ponto, sublinhado e hífen: `[A-Za-z0-9][A-Za-z0-9._-]{2,63}`. A implementação não requer nome civil, e-mail ou CPF.

O token opaco é gerado com 32 bytes aleatórios e codificado em base64url sem preenchimento: exatamente 43 caracteres. O cliente o envia em `Authorization: Bearer <token>`. A tabela `auth_principals` guarda apenas seu hash em `token_hash`, nunca a credencial utilizável.

O token utilizável é entregue somente na criação ou rotação. Listagens não expõem token nem hash. Não há sessão por cookie nem senha compartilhada entre usuários. Perder o token exige que um administrador ativo o rotacione; não existe recuperação automática por e-mail.

`POST /api/admin/principals/{id}/rotate-token` também reativa um principal revogado, preservando seu UUID e seus dados. O token anterior deixa de servir para novas requisições. Se o administrador rotacionar o próprio token, deverá usar o novo a partir da requisição seguinte.

### Bootstrap

- `APP_BOOTSTRAP_ADMIN_TOKEN`: segredo local gerado com fonte criptográfica, com exatamente 43 caracteres e 32 bytes decodificados em base64url canônico. Entradas com menos de 12 caracteres distintos são recusadas como repetitivas. Essa checagem não mede entropia nem permite substituir a geração aleatória por uma sequência manual.
- `APP_BOOTSTRAP_ADMIN_USERNAME`: identificador operacional do primeiro administrador; padrão `bootstrap-admin`.
- A aplicação precisa de um administrador ativo para servir a API. Sem ele e sem bootstrap válido na primeira instalação, a inicialização falha.
- O bootstrap é exclusivo da primeira instalação. Alterar a variável não altera a credencial de um administrador existente.
- Depois de criar o administrador e guardar sua credencial, o token de bootstrap pode ser retirado das variáveis do serviço. A inicialização posterior exige que continue existindo administrador ativo; o bootstrap não recupera contas preexistentes.
- Guarde a credencial administrativa em um gerenciador de segredos antes de encerrar a sessão que a gerou. Não publique o token e não o use como exemplo fixo.

Consulte o README para gerar o segredo localmente em PowerShell. O administrador deve entregar os tokens individuais somente ao destinatário correto, por canal privado.

### Acesso local e ponto de entrada remoto

A execução direta usa `server.address=${SERVER_ADDRESS:127.0.0.1}`: a API escuta apenas a interface local por padrão. O mapeamento do Compose `127.0.0.1:8080:8080` também publica a porta somente na interface local da máquina que executa Docker. O Compose define `SERVER_ADDRESS=0.0.0.0` dentro do contêiner para viabilizar o encaminhamento, sem abrir a porta publicada no host à rede externa.

Acesso remoto depende de um proxy reverso HTTPS configurado deliberadamente. Sobrescreva `SERVER_ADDRESS` na execução direta somente se o proxy exigir outra interface, mantendo bloqueado o acesso público direto à porta HTTP. A autenticação individual continua obrigatória atrás do proxy. No Compose, a variável de bootstrap aceita ficar vazia para reinicializações após a primeira instalação; isso não desabilita a exigência de autenticação nem permite iniciar um banco novo sem administrador.

## Matriz de autorização

| Operação | Anônimo | USER ativo | ADMIN ativo |
|---|---|---|---|
| Probe `/actuator/health` e subcaminhos | Permitido | Permitido | Permitido |
| Outros endpoints Actuator expostos | Negado | Negado | Permitido |
| Criar/listar/resumir transações | Negado | Próprio escopo | Próprio escopo |
| Excluir transação por UUID | Negado | Somente própria | Somente própria |
| Assistente texto/voz e ferramentas | Negado | Próprio escopo | Próprio escopo |
| Listar auditorias da IA | Negado | Somente próprias | Somente próprias |
| Listar/criar principais | Negado | Negado | Permitido |
| Rotacionar/revogar credencial | Negado | Negado | Permitido, com proteção administrativa |
| Ler registros legados sem proprietário | Negado | Negado | Negado |

O papel de administrador controla identidades e credenciais. Ele não confere acesso geral aos dados financeiros. O proprietário vem da autenticação; não pode ser selecionado por campo do corpo HTTP ou pelo texto enviado à IA. Revogação bloqueia novas requisições autenticadas, sem excluir os dados do principal. A própria conta e o último administrador ativo não podem ser revogados.

A console H2 fica desabilitada. Caminhos fora de `/api/**` e dos endpoints Actuator permitidos são negados. A mesma política se aplica aos modos local e OpenAI; não há configuração para liberar a API sem autenticação.

## Migração sem atribuir titular por inferência

O Flyway executa as migrações versionadas de `src/main/resources/db/migration`. A versão 1 representa o esquema legado; a versão 2 acrescenta identidade e vínculos de proprietário. Hibernate valida o resultado, sem atualizar tabelas automaticamente. `baselineOnMigrate` permanece desabilitado.

### Banco vazio

Com as credenciais locais configuradas, a primeira inicialização executa as migrações e cria o primeiro administrador. Não é necessário baseline manual.

### Banco existente sem histórico Flyway

Esta é uma operação separada, a executar pelo responsável do banco após inspeção. Não rode os comandos abaixo em produção apenas porque aparecem neste documento.

1. Pare todos os escritores, incluindo a versão anterior da API. Identifique URL, banco e esquema efetivos, sem publicar credenciais. Registre as versões e a estrutura real das tabelas, os volumes de linhas e as dependências adicionais.
2. Faça backup consistente com as ferramentas do banco e comprove a restauração em uma cópia isolada. Para H2 em arquivo, encerre as conexões antes de copiar os arquivos; para PostgreSQL, use uma rotina de backup/restauração validada pelo operador.
3. Compare o esquema restaurado com V1 por coluna, tipo, nulabilidade, chave, índice e relacionamento. Use o arquivo SQL versionado como fonte definitiva e confira todos os campos abaixo. Se houver divergência, interrompa e prepare uma migração específica; não marque o esquema como versão 1 por tentativa.

| Tabela legada | Colunas a conferir no preflight |
|---|---|
| `financial_transactions` | `id`, `type`, `transaction_amount`, `description`, `category`, `occurred_at`, `created_at` |
| `ai_interactions` | `id`, `channel`, `mode`, `input_text`, `response_text`, `status`, `error_message`, `created_at` |

4. Se a equivalência com V1 estiver comprovada, registre explicitamente o baseline **1** na cópia, usando a distribuição do Flyway compatível com o projeto e o driver do banco. Não use baseline em banco vazio nem o execute novamente onde já existe histórico.

Exemplo PowerShell para a ferramenta CLI Flyway já instalada e banco **de validação** selecionado. As variáveis `DATABASE_*` devem apontar para essa cópia; a senha é enviada por ambiente, não como argumento de linha de comando:

```powershell
$env:FLYWAY_URL = $env:DATABASE_URL
$env:FLYWAY_USER = $env:DATABASE_USERNAME
$env:FLYWAY_PASSWORD = $env:DATABASE_PASSWORD
try {
  flyway -baselineVersion=1 -baselineOnMigrate=false baseline
  if ($LASTEXITCODE -ne 0) { throw 'Baseline falhou; não iniciar a aplicação.' }
} finally {
  Remove-Item Env:FLYWAY_PASSWORD
}
```

5. Inicie a nova versão contra a cópia. O Flyway aplica V2 e Hibernate valida. Confirme as contagens, as restrições e o resultado dos testes de acesso. As linhas preexistentes continuam com `owner_id = NULL`, preservadas no banco e invisíveis a todas as APIs, inclusive para administradores.
6. Somente depois de ensaiar a migração e a restauração, planeje uma janela operacional para repetir o procedimento aprovado no banco real. Esta entrega não executa essa janela.

### Legado em quarentena

`owner_id = NULL` não significa dado público, conta padrão ou propriedade do administrador. A aplicação não infere titular de e-mails, nomes, descrições financeiras, textos de IA ou CPF, nem fornece endpoint para atribuir o legado. A vinculação exige evidência externa verificada e uma futura operação controlada e revisada. Não libere registros sem titular autenticado para contornar a quarentena.

### Retorno e recuperação

A versão anterior não aplica isolamento de proprietários; não a recoloque em serviço sobre o banco migrado. Se for necessário abandonar a migração, mantenha o serviço fechado, preserve uma cópia para análise e restaure o backup validado em ambiente isolado. A retomada com dados pessoais exige que o controle de acesso esteja preservado. Não apague o histórico Flyway para forçar nova inicialização.

## Verificação de aceitação

- Sem token, tokens inválidos e tokens revogados não acessam `/api/**`, independentemente do perfil ativo.
- Dois principais ativos não conseguem listar, resumir, excluir ou consultar por IA os dados um do outro.
- O administrador gerencia credenciais, mas não consegue ler dados financeiros de outros principais.
- Dados novos recebem proprietário a partir da autenticação; o modelo não decide quem é o proprietário.
- Linhas legadas sem proprietário continuam existentes no banco e não aparecem na API.
- Criar ou rotacionar principal revela o token uma única vez; listagens não revelam hash ou token.
- O bootstrap ausente, curto, não canônico ou com menos de 12 caracteres distintos é recusado em uma instalação sem administrador; a geração criptograficamente aleatória é responsabilidade do operador.
- Migração em banco vazio e falha em banco legado sem baseline são verificadas separadamente.

## Pendências fora desta etapa

Continuam sem implementação nesta etapa: tela administrativa, finalidade e ciência versionada, canal/política de privacidade, exportação por titular, exclusão/anonimização integral, retenção legal, minimização de logs e conteúdo de auditoria. A integração OpenAI e o conteúdo enviado ao provedor permanecem no escopo da auditoria anterior. Revogar credencial não é atender a um pedido de eliminação.
