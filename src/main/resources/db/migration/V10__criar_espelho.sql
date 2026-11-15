CREATE TABLE Espelho (
                         id_espelho   BIGINT      NOT NULL AUTO_INCREMENT,
                         ocs_id       BIGINT      NOT NULL,
                         status       VARCHAR(13) NOT NULL DEFAULT 'EM_CONSTRUCAO',
                         data_criacao DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         criado_por   BIGINT      NOT NULL,
                         data_envio   DATETIME    NULL,
                         enviado_por  BIGINT      NULL,

                         CONSTRAINT pk_espelho PRIMARY KEY (id_espelho),
                         CONSTRAINT fk_espelho_ocs FOREIGN KEY (ocs_id) REFERENCES Ocs (id_ocs),
                         CONSTRAINT fk_espelho_criado_por FOREIGN KEY (criado_por) REFERENCES Usuario (id_usuario),
                         CONSTRAINT fk_espelho_enviado_por FOREIGN KEY (enviado_por) REFERENCES Usuario (id_usuario),
                         CONSTRAINT ck_espelho_status CHECK (status IN ('EM_CONSTRUCAO', 'ENVIADO')),
                         CONSTRAINT ck_espelho_envio CHECK (
                             (status = 'EM_CONSTRUCAO' AND data_envio IS NULL AND enviado_por IS NULL) OR
                             (status = 'ENVIADO' AND data_envio IS NOT NULL AND enviado_por IS NOT NULL)
                             )
);

CREATE TABLE EspelhoItem (
                             id_espelho_item      BIGINT        NOT NULL AUTO_INCREMENT,
                             espelho_id           BIGINT        NOT NULL,
                             guia_procedimento_id BIGINT        NOT NULL,
                             quantidade_sessoes   INT           NOT NULL,
                             valor_sessao         DECIMAL(10,2) NOT NULL,

                             CONSTRAINT pk_espelhoitem PRIMARY KEY (id_espelho_item),
                             CONSTRAINT fk_espelhoitem_espelho FOREIGN KEY (espelho_id) REFERENCES Espelho (id_espelho),
                             CONSTRAINT fk_espelhoitem_guiaprocedimento
                                 FOREIGN KEY (guia_procedimento_id) REFERENCES GuiaProcedimento (id_guia_procedimento),
                             CONSTRAINT uk_espelhoitem_guiaprocedimento UNIQUE (guia_procedimento_id),
                             CONSTRAINT ck_espelhoitem_quantidade CHECK (quantidade_sessoes > 0),
                             CONSTRAINT ck_espelhoitem_valor CHECK (valor_sessao >= 0)
);

ALTER TABLE Guia
    ADD COLUMN espelho_id BIGINT NULL,
    ADD CONSTRAINT fk_guia_espelho FOREIGN KEY (espelho_id) REFERENCES Espelho (id_espelho);