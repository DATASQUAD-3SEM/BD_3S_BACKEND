ALTER TABLE EspelhoItem
    ADD COLUMN valor_contrato_sessao DECIMAL(10,2) NULL,
    ADD COLUMN origem_contrato VARCHAR(16) NULL,
    ADD COLUMN observacao VARCHAR(500) NULL,
    ADD CONSTRAINT ck_espelhoitem_origem_contrato CHECK (
        origem_contrato IS NULL OR origem_contrato IN ('TABELA', 'TABELA_CORRIGIDA', 'CONFERIDO', 'MANUAL')),
    ADD CONSTRAINT ck_espelhoitem_contrato_par CHECK (
        (valor_contrato_sessao IS NULL AND origem_contrato IS NULL)
        OR (valor_contrato_sessao IS NOT NULL AND origem_contrato IS NOT NULL));

CREATE TABLE OcsPreco (
                          id_ocs_preco   BIGINT        NOT NULL AUTO_INCREMENT,
                          ocs_id         BIGINT        NOT NULL,
                          cod_dgp        VARCHAR(10)   NOT NULL,
                          valor_sessao   DECIMAL(10,2) NOT NULL,
                          atualizado_em  DATETIME      NULL,
                          atualizado_por BIGINT        NULL,

                          CONSTRAINT pk_ocspreco PRIMARY KEY (id_ocs_preco),
                          CONSTRAINT fk_ocspreco_ocs FOREIGN KEY (ocs_id) REFERENCES Ocs (id_ocs),
                          CONSTRAINT fk_ocspreco_atualizado_por FOREIGN KEY (atualizado_por) REFERENCES Usuario (id_usuario),
                          CONSTRAINT uk_ocspreco_ocs_cod UNIQUE (ocs_id, cod_dgp),
                          CONSTRAINT ck_ocspreco_cod_dgp CHECK (REGEXP_LIKE(cod_dgp, '^[0-9]+$')),
                          CONSTRAINT ck_ocspreco_valor CHECK (valor_sessao >= 0),
                          CONSTRAINT ck_ocspreco_atualizacao CHECK ((atualizado_em IS NULL) = (atualizado_por IS NULL))
);