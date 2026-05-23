CREATE TABLE usuarios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(60) NOT NULL,
    email_cifrado VARBINARY(512) NOT NULL,
    email_hash VARCHAR(64) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    nome_cifrado VARBINARY(512),
    ativo TINYINT(1) NOT NULL DEFAULT 1,
    falhas_login INT NOT NULL DEFAULT 0,
    bloqueado_ate DATETIME NULL,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuarios_username UNIQUE (username),
    CONSTRAINT uk_usuarios_email_hash UNIQUE (email_hash)
);

CREATE TABLE usuario_roles (
    usuario_id BIGINT NOT NULL,
    role VARCHAR(30) NOT NULL,
    CONSTRAINT pk_usuario_roles PRIMARY KEY (usuario_id, role),
    CONSTRAINT fk_usuario_roles_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE
);

CREATE TABLE refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expira_em DATETIME NOT NULL,
    revogado TINYINT(1) NOT NULL DEFAULT 0,
    criado_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE,
    INDEX idx_refresh_usuario (usuario_id)
);
