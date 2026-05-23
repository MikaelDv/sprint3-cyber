CREATE TABLE veiculos (

                          id BIGINT AUTO_INCREMENT PRIMARY KEY,

                          marca VARCHAR(100) NOT NULL,

                          modelo VARCHAR(100) NOT NULL,

                          versao VARCHAR(100) NOT NULL,

                          CONSTRAINT uk_veiculo UNIQUE (marca, modelo, versao)
);

CREATE TABLE veiculo_especificacoes (

                                        veiculo_id BIGINT NOT NULL,

                                        nome VARCHAR(255),

                                        valor VARCHAR(255),

                                        CONSTRAINT fk_veiculo
                                            FOREIGN KEY (veiculo_id)
                                                REFERENCES veiculos(id)
                                                ON DELETE CASCADE
);