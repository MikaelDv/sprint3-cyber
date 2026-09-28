# Car Search App

Projeto desenvolvido para a sprint de **Arquitetura Orientada a Serviços
e Web Services** e estendido na sprint de **Cybersecurity**, utilizando
**Spring Boot 4**, **React Native**, **MySQL** e integração com a
**Gemini API**.

A aplicação permite consultar, cadastrar e pesquisar veículos através de
uma API RESTful **autenticada por JWT**, integrada a um aplicativo mobile
desenvolvido em React Native.

---

# Arquitetura da Aplicação

## Diagrama da Solução

<img width="1536" height="1024" alt="ChatGPT Image 15 de mai  de 2026, 07_29_03" src="https://github.com/user-attachments/assets/84777667-24a1-4041-b348-4cc62d1ec32f" />

---

# Tecnologias Utilizadas

## 🔹 Backend
- Java 21
- Spring Boot 4 (Web, WebFlux, Data JPA, Validation, **Security**, Actuator)
- **JWT (jjwt 0.12)**
- **Bucket4j** (rate limiting)
- **Jsoup** (sanitização anti-XSS)
- MySQL 8
- Maven
- Swagger / OpenAPI
- Flyway

## 🔹 APIs Externas
- Gemini API (Google AI)

---

# Arquitetura do Projeto

Arquitetura em camadas (SOA) com adição de camadas de segurança
transversal:

```text
Controller → Service → Repository → Banco de Dados
                ↑
       Security Filters: Headers → RateLimit → Suspicious → HMAC → JWT
```

## Estrutura do Backend

```text
src/main/java/.../challengeFord
 ┣ config           → OpenAPI, registro de filtros, seed do admin
 ┣ controller       → REST controllers (Veiculo, Auth)
 ┣ crypto           → AES-GCM, HMAC, AttributeConverter
 ┣ dto              → DTOs validados
 ┣ exception        → GlobalExceptionHandler + ApiError padronizado
 ┣ model            → Veiculo, Usuario, Role, RefreshToken, AuditLog
 ┣ repository       → Spring Data JPA
 ┣ security         → SecurityConfig, JwtService, filtros customizados
 ┣ service          → Regras de negócio + AuthService + AuditService
 ┗ util             → InputSanitizer, LogSanitizer
```

---

# Endpoints da API

> **Todos os endpoints (exceto `/auth/login`, `/auth/refresh`,
> `/actuator/health`, `/actuator/info` e Swagger) exigem `Authorization:
> Bearer <token>` e respeitam RBAC.**

## Autenticação

```http
POST /auth/login         (público)
POST /auth/refresh       (público)
POST /auth/registrar     (somente ADMIN)
```

## Veículos

```http
GET  /veiculos                 (ADMIN, ANALISTA, USER)
GET  /veiculos/buscar?...      (ADMIN, ANALISTA, USER)
POST /veiculos                 (ADMIN, ANALISTA)
POST /veiculos/consultar       (ADMIN, ANALISTA — exige assinatura HMAC)
```

Exemplo de body para `POST /veiculos`:

```json
{
  "marca": "Ford",
  "modelo": "Mustang",
  "versao": "GT"
}
```

Exemplo de body para `POST /veiculos/consultar`:

```json
{
  "marca": "Ford",
  "modelo": "Mustang",
  "versao": "GT",
  "prompt": "Qual a potência desse veículo?"
}
```

---

# Banco de Dados e Migrações

MySQL 8 + Flyway. As migrations são aplicadas automaticamente no
primeiro start:

| Versão | Conteúdo |
|--------|----------|
| V1     | Tabelas `veiculos` e `veiculo_especificacoes` |
| V2     | Tabelas `usuarios`, `usuario_roles`, `refresh_tokens` |
| V3     | Tabela `audit_log` |
| V4     | Colunas de auditoria + ajuste de tamanhos em `veiculos` |

---

# Como Executar o Projeto

## Pré-requisitos
- **JDK 21** — confirme com `java -version`. Se aparecer outra versão
  (ex.: Java 8), ajuste `JAVA_HOME` e o `PATH` para apontarem para o
  JDK 21 antes de continuar. O Spring Boot 4 **não compila com versões
  anteriores ao Java 21**.
- MySQL 8 rodando local (ou em container)
- Git

## 1. Clonar o repositório

```bash
git clone https://github.com/MikaelDv/challenge-cyber.git
cd challenge-cyber
```

## 2. Criar o banco

```sql
CREATE DATABASE carsearch CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 3. Configurar o arquivo `.env`

Crie um `.env` na raiz do projeto. O Spring Boot 4 lê o arquivo
nativamente via `spring.config.import=optional:file:./.env[.properties]`
(declarado em `application.properties`) — o formato `KEY=VALUE` é
idêntico ao `.properties`, então não há lib externa envolvida.

```env
# Banco
DB_URL=jdbc:mysql://localhost:3306/carsearch
DB_USER=root
DB_PASSWORD=senha

# IA
GEMINI_API_KEY=sua_chave_gemini

# Servidor (HTTP em dev; HTTPS em produção)
SERVER_PORT=8080
SERVER_SSL_ENABLED=false

# CORS
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8081

# Swagger
SWAGGER_ENABLED=true

# Seed do primeiro ADMIN (executa só se a tabela usuarios estiver vazia)
BOOTSTRAP_ADMIN_ENABLED=true
BOOTSTRAP_ADMIN_USERNAME=admin
BOOTSTRAP_ADMIN_EMAIL=admin@challenge-ford.local
BOOTSTRAP_ADMIN_PASSWORD=Admin@2026Senha!
```

> Em **produção** sobrescreva também `JWT_SECRET`, `AES_KEY` e
> `HMAC_SECRET` (todos em Base64 com pelo menos 32 bytes).

## 4. Executar a aplicação

Linux / macOS:

```bash
./mvnw spring-boot:run
```

Windows (PowerShell ou CMD):

```powershell
.\mvnw.cmd spring-boot:run
```

No primeiro boot o **Flyway** aplica as 4 migrations e o
`BootstrapAdminRunner` cria o usuário ADMIN inicial. Procure no log:

```
o.f.core.internal.command.DbMigrate  : Successfully applied 4 migrations to schema `carsearch`
...
WARN Bootstrap admin criado com username='admin'. TROQUE A SENHA IMEDIATAMENTE.
Tomcat started on port 8080 (http)
Started ChallengeFordApplication in X.XXX seconds
```

> Para confirmar que o Flyway rodou, no MySQL:
> `USE carsearch; SELECT version, description, success FROM flyway_schema_history;`
> Devem aparecer 4 linhas, todas com `success=1`.

## 5. Obter um token e testar

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","senha":"Admin@2026Senha!"}'
```

Use o `accessToken` retornado em `Authorization: Bearer <token>` nas
demais chamadas.

📘 **Para o tutorial completo de testes (login, RBAC, HMAC,
rate-limit, brute-force, auditoria) veja [`TESTE.md`](TESTE.md).**

🔐 **Para a descrição detalhada dos controles de segurança implementados
veja [`SECURITY.md`](SECURITY.md).**

---

# Swagger / OpenAPI

```text
http://localhost:8080/swagger-ui.html
```

Clique em **Authorize** e cole `Bearer <accessToken>` para acessar os
endpoints protegidos.

---

# Tratamento de Exceções

Tratamento global via `GlobalExceptionHandler`, sem expor stack trace,
classe ou tecnologia interna:

```json
{
  "timestamp": "2026-05-22T20:31:14Z",
  "status": 400,
  "erro": "Dados inválidos",
  "detalhes": ["marca: Marca contém caracteres inválidos"],
  "requestId": "8e3c..."
}
```

---

# Padrões e Boas Práticas Aplicadas

- API RESTful
- Arquitetura SOA
- Separação em camadas + camada de segurança transversal
- Uso correto de métodos HTTP
- DTOs validados (Bean Validation)
- Tratamento global de exceções (sem vazamento)
- Integração com serviço externo
- Persistência com JPA + AttributeConverter (criptografia em repouso)
- Controle de migrações com Flyway
- Documentação com Swagger + JWT bearer
- **Autenticação JWT + RBAC**
- **Rate limiting + detecção de brute-force**
- **Assinatura HMAC para integridade de payload**
- **Trilha de auditoria persistida + logs JSON estruturados**
- **Política de retenção e anonimização de dados**

---

# Integrantes do Grupo

| Nome | RM |
|------|------|
| Pietro Vitor Pezzente | RM557283 |
| Eric Darakjian | RM557082 |
| Kauã Soares Guimarães | RM559044 |
| Enzo Mikael Sanches | RM558887 |
