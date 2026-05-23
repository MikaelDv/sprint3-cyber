CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    ocorrido_em DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor VARCHAR(60) NULL,
    actor_roles VARCHAR(200) NULL,
    ip VARCHAR(45) NULL,
    metodo VARCHAR(10) NULL,
    recurso VARCHAR(200) NULL,
    acao VARCHAR(60) NOT NULL,
    resultado VARCHAR(20) NOT NULL,
    detalhe VARCHAR(500) NULL,
    request_id VARCHAR(64) NULL,
    INDEX idx_audit_ocorrido (ocorrido_em),
    INDEX idx_audit_actor (actor),
    INDEX idx_audit_acao (acao)
);
