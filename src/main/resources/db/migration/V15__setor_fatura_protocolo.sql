ALTER TABLE Usuario DROP CHECK ck_usuario_perfil;
ALTER TABLE Usuario
    ADD CONSTRAINT ck_usuario_perfil
        CHECK (perfil IN ('BENEFICIARIO', 'MEDICO', 'CHEFE_FUSEX', 'SETOR_GUIAS', 'RECEPCAO', 'OCS',
                          'SETOR_FATURA', 'RASTREABILIDADE', 'AUDITORIA'));

ALTER TABLE Usuario DROP CHECK ck_usuario_assinatura_id;
ALTER TABLE Usuario
    ADD CONSTRAINT ck_usuario_assinatura_id
        CHECK (perfil NOT IN ('SETOR_GUIAS', 'RECEPCAO', 'OCS', 'SETOR_FATURA', 'RASTREABILIDADE', 'AUDITORIA')
            OR assinatura_id IS NULL);

ALTER TABLE Espelho
    ADD COLUMN situacao_fatura VARCHAR(14) NULL,
    ADD COLUMN numero_fatura VARCHAR(8) NULL,
    ADD COLUMN protocolada_por BIGINT NULL,
    ADD CONSTRAINT fk_espelho_protocolada_por FOREIGN KEY (protocolada_por) REFERENCES Usuario (id_usuario),
    ADD CONSTRAINT uk_espelho_ocs_numero_fatura UNIQUE (ocs_id, numero_fatura);

UPDATE Espelho SET situacao_fatura = 'RECEBIDO' WHERE status = 'ENVIADO';

ALTER TABLE Espelho
    ADD CONSTRAINT ck_espelho_situacao_fatura CHECK (
        situacao_fatura IS NULL OR
        situacao_fatura IN ('RECEBIDO', 'PROTOCOLADA', 'EM_LISURA', 'AGUARDANDO_OCS', 'EM_AUDITORIA', 'AUDITADA')),
    ADD CONSTRAINT ck_espelho_numero_fatura CHECK (
        numero_fatura IS NULL OR REGEXP_LIKE(numero_fatura, '^[0-9]{3}/[0-9]{4}$')),
    ADD CONSTRAINT ck_espelho_fatura CHECK (
        (status = 'EM_CONSTRUCAO' AND situacao_fatura IS NULL
            AND numero_fatura IS NULL AND protocolada_por IS NULL) OR
        (status = 'ENVIADO' AND situacao_fatura = 'RECEBIDO'
            AND numero_fatura IS NULL AND protocolada_por IS NULL) OR
        (status = 'ENVIADO'
            AND situacao_fatura IN ('PROTOCOLADA', 'EM_LISURA', 'AGUARDANDO_OCS', 'EM_AUDITORIA', 'AUDITADA')
            AND numero_fatura IS NOT NULL AND protocolada_por IS NOT NULL));

CREATE TABLE FaturaEvento (
                              id_fatura_evento BIGINT       NOT NULL AUTO_INCREMENT,
                              espelho_id       BIGINT       NOT NULL,
                              tipo             VARCHAR(18)  NOT NULL,
                              usuario_id       BIGINT       NOT NULL,
                              responsavel      VARCHAR(100) NOT NULL,
                              data_evento      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

                              CONSTRAINT pk_faturaevento PRIMARY KEY (id_fatura_evento),
                              CONSTRAINT fk_faturaevento_espelho FOREIGN KEY (espelho_id) REFERENCES Espelho (id_espelho),
                              CONSTRAINT fk_faturaevento_usuario FOREIGN KEY (usuario_id) REFERENCES Usuario (id_usuario),
                              CONSTRAINT ck_faturaevento_tipo CHECK (
                                  tipo IN ('PROTOCOLADA', 'ENVIADA_LISURA', 'ENVIADA_AUDITORIA', 'VISUALIZADA')),
                              CONSTRAINT ck_faturaevento_responsavel CHECK (CHAR_LENGTH(TRIM(responsavel)) BETWEEN 3 AND 100)
);

CREATE INDEX ix_faturaevento_espelho ON FaturaEvento (espelho_id, data_evento);