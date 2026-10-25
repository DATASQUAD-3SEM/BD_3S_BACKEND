CREATE TABLE GuiaRascunho (
                              pre_guia_id      BIGINT       NOT NULL,
                              numero           VARCHAR(20)  NOT NULL,
                              arquivo_url      VARCHAR(500) NOT NULL,
                              nome_arquivo     VARCHAR(255) NOT NULL,
                              data_atualizacao DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT pk_guia_rascunho PRIMARY KEY (pre_guia_id),
                              CONSTRAINT fk_guia_rascunho_preguia FOREIGN KEY (pre_guia_id) REFERENCES PreGuia (id_pre_guia)
);