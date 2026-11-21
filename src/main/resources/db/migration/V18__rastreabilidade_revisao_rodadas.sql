-- Revisao da T25 (SCRUM-234 / SCRUM-229 / SCRUM-231): migracao nova, a V17 ja aplicada nao e alterada.
-- Decisoes do cliente: decisao por Guia (T27) e rodadas guardadas por linha (andamento da glosa).

-- OcsPreco: descricao do procedimento (mostrada na lisura).
ALTER TABLE OcsPreco
    ADD COLUMN descricao VARCHAR(300) NULL;

-- EspelhoItem: decisao da OCS por linha (preenchida pela T27 e refletida na rodada).
ALTER TABLE EspelhoItem
    ADD COLUMN decisao_ocs VARCHAR(10) NULL,
    ADD COLUMN motivo_contestacao_ocs VARCHAR(500) NULL,
    ADD CONSTRAINT ck_espelhoitem_decisao_ocs CHECK (
        decisao_ocs IS NULL OR decisao_ocs IN ('OK', 'CONTESTADA')),
    ADD CONSTRAINT ck_espelhoitem_motivo_contestacao CHECK (
        (decisao_ocs = 'CONTESTADA' AND CHAR_LENGTH(TRIM(motivo_contestacao_ocs)) BETWEEN 10 AND 500)
        OR (NOT (decisao_ocs <=> 'CONTESTADA') AND motivo_contestacao_ocs IS NULL)),
    ADD CONSTRAINT ck_espelhoitem_conferido CHECK (
        origem_contrato <> 'CONFERIDO' OR valor_contrato_sessao = valor_sessao);

-- Historico das rodadas da OCS: uma linha por EspelhoItem enviado em cada ENVIADA_OCS.
CREATE TABLE EspelhoItemRodadaOcs (
    id_rodada_ocs          BIGINT        NOT NULL AUTO_INCREMENT,
    espelho_item_id        BIGINT        NOT NULL,
    envio_evento_id        BIGINT        NOT NULL,
    valor_contrato_sessao  DECIMAL(10,2) NOT NULL,
    origem_contrato        VARCHAR(16)   NOT NULL,
    decisao_ocs            VARCHAR(10)   NULL,
    motivo_contestacao_ocs VARCHAR(500)  NULL,
    decisao_evento_id      BIGINT        NULL,

    CONSTRAINT pk_espelhoitemrodadaocs PRIMARY KEY (id_rodada_ocs),
    CONSTRAINT fk_rodadaocs_item FOREIGN KEY (espelho_item_id) REFERENCES EspelhoItem (id_espelho_item),
    CONSTRAINT fk_rodadaocs_envio FOREIGN KEY (envio_evento_id) REFERENCES FaturaEvento (id_fatura_evento),
    CONSTRAINT fk_rodadaocs_decisao FOREIGN KEY (decisao_evento_id) REFERENCES FaturaEvento (id_fatura_evento),
    CONSTRAINT uk_rodadaocs_item_envio UNIQUE (espelho_item_id, envio_evento_id),
    CONSTRAINT ck_rodadaocs_origem CHECK (origem_contrato IN ('TABELA', 'TABELA_CORRIGIDA', 'CONFERIDO', 'MANUAL')),
    CONSTRAINT ck_rodadaocs_decisao CHECK (decisao_ocs IS NULL OR decisao_ocs IN ('OK', 'CONTESTADA')),
    CONSTRAINT ck_rodadaocs_motivo CHECK (
        (decisao_ocs = 'CONTESTADA' AND CHAR_LENGTH(TRIM(motivo_contestacao_ocs)) BETWEEN 10 AND 500)
        OR (NOT (decisao_ocs <=> 'CONTESTADA') AND motivo_contestacao_ocs IS NULL)),
    CONSTRAINT ck_rodadaocs_decisao_evento CHECK (decisao_ocs IS NULL OR decisao_evento_id IS NOT NULL)
);
