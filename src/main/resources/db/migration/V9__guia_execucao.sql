-- V8: execucao da Guia (T18-BE1). Colunas, GuiaProcedimento e Sessao, conforme o modelo logico (secoes 2.2 a 2.4).
-- validade_ate entra NULL: a T16 (rotina de correcao) preenche as Guias antigas, e a segunda migration da T16 a torna NOT NULL.
-- GuiaProcedimento entra sem valor_unitario preenchido (a T22 grava os exames ao concluir a Guia).
-- SQL escrito para funcionar em MySQL 8 E em H2 (modo MySQL).

-- =====================================================================
-- Guia: validade (NAO RECEBER APOS), envio a OCS e situacao de execucao
-- =====================================================================
ALTER TABLE Guia
    ADD COLUMN validade_ate DATE NULL,
    ADD COLUMN data_envio DATETIME NULL,
    ADD COLUMN enviada_por BIGINT NULL,
    ADD COLUMN situacao_execucao VARCHAR(20) NOT NULL DEFAULT 'NAO_ENVIADA',
    ADD CONSTRAINT fk_guia_enviada_por FOREIGN KEY (enviada_por) REFERENCES Usuario (id_usuario),
    ADD CONSTRAINT ck_guia_envio CHECK (data_envio IS NULL OR enviada_por IS NOT NULL),
    ADD CONSTRAINT ck_guia_situacao_execucao
        CHECK (situacao_execucao IN ('NAO_ENVIADA', 'ENVIADA', 'EM_EXECUCAO', 'REALIZADA', 'ENCERRADA', 'VENCIDA'));

-- =====================================================================
-- GuiaProcedimento: exames e quantidades autorizadas da Guia
-- =====================================================================
CREATE TABLE GuiaProcedimento (
    id_guia_procedimento   BIGINT        NOT NULL AUTO_INCREMENT,
    guia_id                BIGINT        NOT NULL,
    cod_dgp                VARCHAR(10)   NOT NULL,
    descricao_procedimento VARCHAR(500)  NOT NULL,
    quantidade_autorizada  INT           NOT NULL,
    valor_unitario         DECIMAL(10,2) NULL,

    CONSTRAINT pk_guiaprocedimento PRIMARY KEY (id_guia_procedimento),
    CONSTRAINT fk_guiaprocedimento_guia FOREIGN KEY (guia_id) REFERENCES Guia (id_guia),
    CONSTRAINT ck_guiaprocedimento_cod_dgp CHECK (REGEXP_LIKE(cod_dgp, '^[0-9]+$')),
    CONSTRAINT uk_guiaprocedimento_guia_cod UNIQUE (guia_id, cod_dgp),
    CONSTRAINT ck_guiaprocedimento_valor CHECK (valor_unitario IS NULL OR valor_unitario >= 0),
    CONSTRAINT ck_guiaprocedimento_quantidade CHECK (quantidade_autorizada > 0)
);

-- =====================================================================
-- Sessao: uma linha por unidade de quantidade_autorizada (criada no envio da Guia, T18-BE2)
-- =====================================================================
CREATE TABLE Sessao (
    id_sessao            BIGINT      NOT NULL AUTO_INCREMENT,
    guia_procedimento_id BIGINT      NOT NULL,
    numero               INT         NOT NULL,
    status               VARCHAR(12) NOT NULL DEFAULT 'DISPONIVEL',
    data_solicitacao     DATETIME    NULL,
    solicitada_por       BIGINT      NULL,
    data_assinatura      DATETIME    NULL,
    assinado_por         BIGINT      NULL,
    assinatura_id        BIGINT      NULL,

    CONSTRAINT pk_sessao PRIMARY KEY (id_sessao),
    CONSTRAINT fk_sessao_guiaprocedimento
        FOREIGN KEY (guia_procedimento_id) REFERENCES GuiaProcedimento (id_guia_procedimento),
    CONSTRAINT fk_sessao_solicitada_por FOREIGN KEY (solicitada_por) REFERENCES Usuario (id_usuario),
    CONSTRAINT fk_sessao_assinado_por FOREIGN KEY (assinado_por) REFERENCES Usuario (id_usuario),
    CONSTRAINT fk_sessao_assinatura FOREIGN KEY (assinatura_id) REFERENCES Assinatura (id_assinatura),
    CONSTRAINT uk_sessao_procedimento_numero UNIQUE (guia_procedimento_id, numero),
    CONSTRAINT ck_sessao_numero CHECK (numero > 0),
    CONSTRAINT ck_sessao_status
        CHECK (status IN ('DISPONIVEL', 'SOLICITADA', 'ASSINADA', 'EXPIRADA')),
    CONSTRAINT ck_sessao_solicitacao
        CHECK (status IN ('DISPONIVEL', 'EXPIRADA') OR (data_solicitacao IS NOT NULL AND solicitada_por IS NOT NULL)),
    CONSTRAINT ck_sessao_assinatura
        CHECK (status <> 'ASSINADA' OR (data_assinatura IS NOT NULL AND assinado_por IS NOT NULL AND assinatura_id IS NOT NULL))
);
