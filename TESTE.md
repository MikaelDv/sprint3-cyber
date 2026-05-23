# Como Testar a API com a Camada de Segurança

Este guia mostra como subir o projeto, criar o primeiro usuário ADMIN
através do **seed automático**, e exercitar cada controle de segurança
(JWT, RBAC, validação, rate-limit, HMAC, auditoria, etc.).

---

## 1. Pré-requisitos

| Ferramenta | Versão |
|------------|--------|
| JDK        | 21     |
| MySQL      | 8.x    |
| curl       | qualquer (já vem no Windows 10+) |
| PowerShell | 5.1+ ou 7+ |

Crie o banco vazio:

```sql
CREATE DATABASE carsearch CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

---

## 2. Arquivo `.env`

Crie um `.env` na raiz do projeto (o `spring-dotenv` já lê
automaticamente):

```env
# Banco
DB_URL=jdbc:mysql://localhost:3306/carsearch
DB_USER=root
DB_PASSWORD=senha

# IA
GEMINI_API_KEY=sua_chave_gemini

# Servidor (HTTP em dev, HTTPS em prod)
SERVER_PORT=8080
SERVER_SSL_ENABLED=false

# CORS — origens autorizadas (lista CSV)
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8081

# Swagger
SWAGGER_ENABLED=true

# Seed do primeiro ADMIN — só roda se a tabela usuarios estiver vazia
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_EMAIL=admin@challenge-ford.local
BOOTSTRAP_ADMIN_NOME=Administrador
BOOTSTRAP_ADMIN_PASSWORD=Admin@2026Senha!
```

> Os valores de `JWT_SECRET`, `AES_KEY` e `HMAC_SECRET` já têm defaults
> no `application.properties` que servem para desenvolvimento local.
> **Em produção sobrescreva-os via variáveis de ambiente.**

---

## 3. Como funciona o seed (primeiro ADMIN)

Como o endpoint `POST /auth/registrar` **exige token de ADMIN**, é preciso
existir um ADMIN antes de qualquer cadastro. Por isso o projeto inclui o
componente `config/BootstrapAdminRunner.java` (`CommandLineRunner`) que
roda **uma única vez**, no primeiro boot da aplicação:

1. Após o Spring subir e o Flyway aplicar todas as migrations, o runner é
   chamado.
2. Ele consulta `usuarios.count()`. Se houver **qualquer usuário**, não
   faz nada (idempotente — pode reiniciar à vontade).
3. Se a tabela estiver vazia, ele cria um usuário com:
   - `username`, `email`, `nome`, `senha` vindos das variáveis
     `BOOTSTRAP_ADMIN_*` do `.env`.
   - role `ADMIN`.
   - **Senha em BCrypt(12)** e **e-mail criptografado com AES-GCM**
     usando a chave da aplicação (por isso o seed é Java, não SQL —
     assim o ciphertext casa com a chave configurada).
4. Imprime no log:
   ```
   WARN Bootstrap admin criado com username='admin'. TROQUE A SENHA IMEDIATAMENTE.
   ```

### Como desligar o seed

Após criar o primeiro admin "de verdade" via `/auth/registrar`, defina no
`.env`:

```env
BOOTSTRAP_ADMIN_ENABLED=false
```

Ou simplesmente deixe `true` — ele só dispara quando a tabela está vazia.

### Como rotacionar a senha do bootstrap

1. **Login** com a senha bootstrap.
2. Crie outro ADMIN pelo `/auth/registrar` com a senha forte definitiva.
3. Faça login com o novo ADMIN.
4. Desative ou apague o usuário bootstrap pelo banco
   (`UPDATE usuarios SET ativo=0 WHERE username='admin';`).

---

## 4. Subir a aplicação

```powershell
./mvnw.cmd spring-boot:run
```

Verifique no console:

```
... Flyway ... Successfully applied 4 migrations to schema `carsearch`
... Bootstrap admin criado com username='admin'. TROQUE A SENHA IMEDIATAMENTE.
... Started ChallengeFordApplication in X.X seconds
```

A API está disponível em `http://localhost:8080`.

---

## 5. Testando os controles de segurança

Em todos os exemplos abaixo, exporte o token no PowerShell:

```powershell
$BASE = "http://localhost:8080"
```

### 5.1 Login (obtém access + refresh token)

```powershell
$resp = curl.exe -s -X POST "$BASE/auth/login" `
  -H "Content-Type: application/json" `
  -d '{"username":"admin","senha":"Admin@2026Senha!"}'
$resp
```

Resposta:

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "abc123...",
  "tokenType": "Bearer",
  "expiresIn": 900
}
```

Salve o access token:

```powershell
$TOKEN = ($resp | ConvertFrom-Json).accessToken
$REFRESH = ($resp | ConvertFrom-Json).refreshToken
```

### 5.2 Acesso sem token → **401**

```powershell
curl.exe -i "$BASE/veiculos"
```

Esperado: `HTTP/1.1 401 Unauthorized` + corpo `{"erro":"Não autenticado","status":401}`.

### 5.3 Acesso com token válido → **200**

```powershell
curl.exe "$BASE/veiculos" -H "Authorization: Bearer $TOKEN"
```

### 5.4 Cadastrar veículo (RBAC: ADMIN ou ANALISTA)

```powershell
curl.exe -X POST "$BASE/veiculos" `
  -H "Authorization: Bearer $TOKEN" `
  -H "Content-Type: application/json" `
  -d '{"marca":"Ford","modelo":"Mustang","versao":"GT"}'
```

### 5.5 Validação de entrada — XSS rejeitado → **400**

```powershell
curl.exe -X POST "$BASE/veiculos" `
  -H "Authorization: Bearer $TOKEN" `
  -H "Content-Type: application/json" `
  -d '{"marca":"<script>alert(1)</script>","modelo":"X","versao":"Y"}'
```

Esperado:
```json
{"status":400,"erro":"Dados inválidos","detalhes":["marca: Marca contém caracteres inválidos"], ...}
```

### 5.6 Validação — SQL injection rejeitado → **400**

```powershell
curl.exe -X POST "$BASE/veiculos" `
  -H "Authorization: Bearer $TOKEN" `
  -H "Content-Type: application/json" `
  -d "{`"marca`":`"Ford' OR 1=1--`",`"modelo`":`"X`",`"versao`":`"Y`"}"
```

### 5.7 Validação — payload grande demais → **400**

```powershell
$big = "A" * 5000
curl.exe -X POST "$BASE/veiculos" `
  -H "Authorization: Bearer $TOKEN" `
  -H "Content-Type: application/json" `
  -d "{`"marca`":`"$big`",`"modelo`":`"X`",`"versao`":`"Y`"}"
```

### 5.8 Consultar IA — exige assinatura HMAC

A rota `/veiculos/consultar` exige os headers `X-Payload-Timestamp` e
`X-Payload-Signature`. Script PowerShell completo:

```powershell
$body = '{"marca":"Ford","modelo":"Mustang","versao":"GT","prompt":"motor e torque"}'
$ts   = [int][double]::Parse((Get-Date -UFormat %s))
$secret = [Convert]::FromBase64String("cGxlYXNlLWNoYW5nZS1tZS1obWFjLXNlY3JldA==")
$canonical = "$ts`nPOST`n/veiculos/consultar`n$body"

$hmac = New-Object System.Security.Cryptography.HMACSHA256
$hmac.Key = $secret
$sigBytes = $hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($canonical))
$sig = -join ($sigBytes | ForEach-Object { $_.ToString("x2") })

curl.exe -X POST "$BASE/veiculos/consultar" `
  -H "Authorization: Bearer $TOKEN" `
  -H "Content-Type: application/json" `
  -H "X-Payload-Timestamp: $ts" `
  -H "X-Payload-Signature: $sig" `
  -d $body
```

Modifique 1 caractere no `$body` **sem** refazer a assinatura → **400
Assinatura inválida**.

### 5.9 Rate limiting / brute-force

Dispare 10 logins seguidos com senha errada:

```powershell
1..10 | ForEach-Object {
  curl.exe -s -o NUL -w "tentativa $_ -> %{http_code}`n" `
    -X POST "$BASE/auth/login" `
    -H "Content-Type: application/json" `
    -d '{"username":"admin","senha":"errada"}'
}
```

Esperado: as primeiras retornam **400** ("Credenciais inválidas"), a
partir da 6ª retorna **429 Too Many Requests** com header `Retry-After`.

### 5.10 Bloqueio de conta

Após 5 falhas reais (mesma `senha` errada para o mesmo `username`),
a 6ª tentativa, **mesmo com a senha correta**, retorna 400/Credenciais
inválidas porque a conta foi bloqueada por 15 minutos. O log
`logs/security.log` registra o evento.

### 5.11 RBAC negando

Crie um usuário USER comum (logado como ADMIN):

```powershell
curl.exe -X POST "$BASE/auth/registrar" `
  -H "Authorization: Bearer $TOKEN" `
  -H "Content-Type: application/json" `
  -d '{"username":"joao","email":"joao@x.com","nome":"Joao","senha":"OutraSenha@2026!","roles":["USER"]}'
```

Faça login com `joao` e tente cadastrar veículo → **403 Acesso negado**
(porque cadastro exige ADMIN ou ANALISTA).

### 5.12 Refresh token

```powershell
curl.exe -X POST "$BASE/auth/refresh" `
  -H "Content-Type: application/json" `
  -d "{`"refreshToken`":`"$REFRESH`"}"
```

Reutilizar o **mesmo** refresh token → **400** e **todos os refresh tokens
do usuário são revogados** (defesa contra replay).

---

## 6. Conferindo auditoria e logs

Em `logs/` aparecem três arquivos JSON estruturados:

| Arquivo            | Conteúdo |
|--------------------|----------|
| `application.log`  | Logs gerais |
| `audit.log`        | LOGIN, REFRESH, CRIAR_VEICULO, CONSULTAR_IA, CONSULTA_MASSIVA |
| `security.log`     | Payloads suspeitos, brute-force, bloqueios |

Também é possível consultar diretamente no banco:

```sql
SELECT ocorrido_em, actor, acao, resultado, recurso, ip, request_id
FROM audit_log
ORDER BY ocorrido_em DESC
LIMIT 20;
```

---

## 7. Swagger

`http://localhost:8080/swagger-ui.html`

1. Clique em **Authorize**.
2. Cole `Bearer <accessToken>`.
3. Todos os endpoints autenticados ficam acessíveis pela UI.

Em produção, defina `SWAGGER_ENABLED=false` no `.env`.

---

## 8. Seed SQL alternativo (opcional)

Se você precisar inserir um ADMIN diretamente via SQL (ex.: ambiente onde
a aplicação ainda não pode subir), use o seguinte — **lembre que o
`email_cifrado` ficará vazio e precisará ser corrigido depois através do
fluxo normal**:

```sql
-- gere a hash BCrypt(12) da senha em https://bcrypt-generator.com
-- ou via: openssl passwd -bcrypt (linux)
INSERT INTO usuarios (
    username, email_cifrado, email_hash, senha_hash,
    ativo, falhas_login, criado_em, atualizado_em
) VALUES (
    'admin',
    X'00',
    SHA2('admin@challenge-ford.local', 256),
    '$2a$12$COLE_O_HASH_BCRYPT_AQUI',
    1, 0, NOW(), NOW()
);

INSERT INTO usuario_roles (usuario_id, role)
VALUES ((SELECT id FROM usuarios WHERE username='admin'), 'ADMIN');
```

> ⚠️ A coluna `email_cifrado` armazena AES-GCM. Como `X'00'` não é um
> ciphertext válido, **a aplicação não conseguirá carregar essa entidade
> em algumas operações** que leem o campo `email`. Para login funciona
> (a leitura do `email` é tardia), mas o caminho recomendado é usar o
> seed automático (`BootstrapAdminRunner`).

---

## 9. Testes unitários

```powershell
./mvnw.cmd test
```

> `ChallengeFordApplicationTests` precisa de um MySQL acessível. Se quiser
> rodar sem banco, suba o MySQL antes ou crie um profile `test` apontando
> para H2 + desabilitando Flyway.

---

## 10. Checklist rápido de validação por requisito

| Requisito | Como verificar |
|-----------|----------------|
| Validação de entrada | Cenários 5.5, 5.6, 5.7 |
| Sanitização XSS/SQLi | Cenários 5.5, 5.6 |
| Tratamento seguro de erros | Confira que nenhuma resposta 4xx/5xx contém stack trace |
| JWT com expiração | Espere 15 min e refaça uma chamada → 401 |
| RBAC | Cenário 5.11 |
| Rate limit | Cenário 5.9 |
| CORS | Faça uma chamada `OPTIONS` de origem não autorizada → 403 |
| HMAC | Cenário 5.8 |
| Criptografia em repouso | `SELECT email_cifrado FROM usuarios` → bytes binários, não texto |
| Anonimização / retenção | Aguarde o cron diário ou rode manualmente via método utilitário |
| Logs estruturados | Veja `logs/*.log` |
| Auditoria | Consulta SQL na seção 6 |
| Monitoramento suspeito | Faça 5+ requisições 401, observe `security.log` |
