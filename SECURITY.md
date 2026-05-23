# Segurança — Challenge Ford API

Este documento descreve como cada requisito da Sprint de Cybersecurity foi
implementado no projeto.

## 1. Segurança de Entrada e Validação de Dados (20 pts)

- **Bean Validation (Jakarta Validation)** em DTOs e entidades:
  `@NotBlank`, `@Size`, `@Pattern`, `@Email`, `@Valid`. Ex.:
  `dto/ConsultaVeiculoDTO.java`, `dto/RegisterRequestDTO.java`,
  `model/Veiculo.java`.
- **Regex restritiva** para marca / modelo / versão / nome de
  especificação impede caracteres exóticos (`util/InputSanitizer.java`).
- **Sanitização anti-XSS** usando `Jsoup` (`Safelist.none()`) +
  remoção de caracteres de controle (`util/InputSanitizer.stripHtml`).
- **Proteção contra SQL Injection**:
  - Spring Data JPA com queries parametrizadas (sem concatenação).
  - Heurística adicional `containsSqlInjectionPattern` que rejeita
    payloads claramente maliciosos.
- **Proteção contra Command Injection** — `containsCommandInjectionPattern`
  bloqueia metacaracteres (`;`, `&`, `|`, backtick, `$`, `<`, `>`,
  quebras de linha).
- **Limitação de tamanho / payload flooding**:
  - `server.max-http-request-header-size`, `multipart.max-file-size`,
    `max-http-form-post-size`, `max-swallow-size` em
    `application.properties`.
  - `@Size(max=…)` em todos os campos de string.
- **Tratamento seguro de erros** (`exception/GlobalExceptionHandler.java`):
  - Respostas padronizadas via `ApiError` (`timestamp`, `status`,
    `erro`, `detalhes`, `requestId`).
  - **Sem stack trace, sem nome de classe, sem dialeto SQL**.
  - `server.error.include-*=never` no `application.properties`.

## 2. Autenticação e Autorização (20 pts)

- **JWT (HS256)** usando `jjwt 0.12`. Tokens assinados com chave de no
  mínimo 256 bits, com `iss`, `sub`, `jti`, `iat`, `nbf`, `exp` e
  claim `roles` (`security/JwtService.java`).
- **Access token de curta duração** (15 min por padrão) + **refresh token
  rotacionado** (armazenado apenas como SHA-256 — nunca em texto puro)
  via `service/AuthService.java` e `model/RefreshToken.java`.
- **Reutilização de refresh token** revoga todos os tokens do usuário
  (defesa contra replay).
- **Bloqueio temporário após 5 falhas** (`MAX_FALHAS=5`, 15 min).
- **RBAC** com papéis `ADMIN`, `ANALISTA`, `USER` (`model/Role.java`),
  aplicado:
  - declarativamente em `SecurityConfig.authorizeHttpRequests`,
  - por método com `@PreAuthorize` em controladores.
- **`BCryptPasswordEncoder(strength=12)`** para armazenamento de senhas.
- **Endpoints públicos**: apenas `/auth/login`, `/auth/refresh`,
  `/actuator/health`, `/actuator/info`, e Swagger.

## 3. Proteção de APIs e Serviços (20 pts)

- **HTTPS / TLS 1.2+** habilitado via
  `server.ssl.enabled-protocols=TLSv1.2,TLSv1.3` e cipher suites
  modernas (AEAD).
- **HSTS** + `X-Content-Type-Options`, `X-Frame-Options`,
  `Referrer-Policy`, `Cache-Control`, CSP em
  `security/SecurityHeadersFilter.java`.
- **Rate limiting** com Bucket4j
  (`security/RateLimitingFilter.java`):
  - 60 req/min por IP nas rotas comuns,
  - 5 tentativas/min nas rotas `/auth/**` (anti brute-force),
  - retorna `429` + `Retry-After`.
- **CORS** explícito apenas para origens autorizadas em
  `security.cors.allowed-origins` (sem `*`).
- **Assinatura HMAC-SHA256** obrigatória em rotas críticas
  (`security/HmacSignatureFilter.java` + `crypto/HmacSigner.java`).
  - Cabeçalhos `X-Payload-Signature` e `X-Payload-Timestamp`.
  - Comparação **constant-time** (`MessageDigest.isEqual`).
  - Tolerância configurável de relógio (default 300 s) — evita replay.

## 4. Segurança de Dados e Privacidade (25 pts)

- **Criptografia em repouso AES-256-GCM** (`crypto/AesGcmCipher.java`)
  com **IV aleatório** por registro e tag de autenticação de 128 bits.
- **`EncryptedStringConverter`** — `AttributeConverter` JPA aplicado a
  campos sensíveis: `Usuario.email`, `Usuario.nome` (colunas
  `VARBINARY(512)`).
- **Hash determinístico (SHA-256)** para busca de email
  (`Usuario.emailHash`), permitindo unicidade sem expor o valor.
- **Política de retenção e descarte** (`service/DataRetentionService.java`):
  - Cron diário às 03:30.
  - Remove `audit_log` mais antigo que `security.retention.audit-days`.
  - Anonimiza veículos sem atualização há mais de
    `security.retention.veiculo-stale-days` (marca/modelo/versão
    substituídos por `ANON-*`; especificações removidas).
  - Remove refresh tokens expirados.
- **Proteção contra exposição acidental**:
  - `LogSanitizer` mascara e-mails, CPFs, cartões, tokens e remove
    CR/LF (anti log-injection).
  - Mensagens de erro genéricas, sem detalhes internos.
  - `server.error.whitelabel.enabled=false`,
    `management.endpoint.health.show-details=never`,
    Actuator restrito a `health` e `info`.
  - `Server: api` substitui o header padrão.

## 5. Monitoramento, Logs e Auditoria (15 pts)

- **Logs estruturados (JSON, single-line)** em `logback-spring.xml`,
  com `requestId` propagado via MDC pelo
  `SecurityHeadersFilter` e devolvido no header `X-Request-Id`.
- **Three appenders separados**:
  - `application.log` — logs de aplicação,
  - `audit.log` — eventos de auditoria (logger `audit`),
  - `security.log` — eventos suspeitos (logger `security`).
- **Trilha de auditoria persistente** na tabela `audit_log`
  (`service/AuditService.java`), gravada para:
  - LOGIN (sucesso/falha/bloqueio),
  - REFRESH,
  - CRIAR_VEICULO,
  - CONSULTAR_IA,
  - CONSULTA_MASSIVA (paginação com > 200 elementos).
- **Detecção de comportamento suspeito**
  (`security/SuspiciousActivityFilter.java`):
  - padrões maliciosos em path/query
    (`union select`, `<script`, `../`, `${jndi`, `xp_cmdshell`…),
  - contagem de 401/403 por IP — alerta a cada múltiplo de 5.

---

## Variáveis de ambiente esperadas

| Variável | Descrição |
|----------|-----------|
| `DB_URL` | JDBC do MySQL |
| `DB_USER` / `DB_PASSWORD` | Credenciais |
| `JWT_SECRET` | Chave Base64 de 256+ bits |
| `AES_KEY` | Chave Base64 para AES-GCM |
| `HMAC_SECRET` | Chave Base64 para assinatura |
| `CORS_ALLOWED_ORIGINS` | Lista CSV de origens |
| `GEMINI_API_KEY` | Chave da API externa |
| `SERVER_SSL_ENABLED` | `true` em produção |
| `SERVER_SSL_KEYSTORE` / `SERVER_SSL_KEYSTORE_PASSWORD` | PKCS12 |

> **Atenção**: os defaults no `application.properties` existem apenas
> para facilitar o boot em ambiente local. **Em produção todas as chaves
> devem ser fornecidas via variáveis de ambiente ou cofre.**
