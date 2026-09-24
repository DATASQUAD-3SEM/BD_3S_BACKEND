-- V1: modelo logico inicial (Sprint 1)
-- SQL escrito para funcionar em MySQL 8 E em H2 (modo MySQL).
-- NUNCA edite este arquivo depois de mergeado. Para mudar o banco, crie uma migration nova
-- (veja docs/GUIA_BANCO_DE_DADOS.md).

CREATE TABLE procedimento_exame (
    id                               BIGINT        NOT NULL AUTO_INCREMENT,
    codigo_tuss                      VARCHAR(20)   NOT NULL,
    terminologia_procedimento_evento VARCHAR(500)  NOT NULL,
    rol_ans_resolucao_normativa      VARCHAR(100),
    rol_ans                          VARCHAR(100),
    grupo                            VARCHAR(255),
    subgrupo                         VARCHAR(255),
    capitulo                         VARCHAR(255),
    CONSTRAINT pk_procedimento_exame PRIMARY KEY (id)
);

CREATE TABLE ocs (
    id                   BIGINT       NOT NULL AUTO_INCREMENT,
    contrato_num         VARCHAR(50)  NOT NULL,
    nome                 VARCHAR(255) NOT NULL,
    inicio_vigencia      DATE,
    tipo                 VARCHAR(50),
    dias_para_vencimento INT,
    termino_vigencia     DATE,
    CONSTRAINT pk_ocs PRIMARY KEY (id)
);

-- Tabela associativa "Realiza": quais procedimentos cada OCS realiza
CREATE TABLE ocs_procedimento (
    ocs_id          BIGINT NOT NULL,
    procedimento_id BIGINT NOT NULL,
    CONSTRAINT pk_ocs_procedimento PRIMARY KEY (ocs_id, procedimento_id),
    CONSTRAINT fk_ocsproc_ocs FOREIGN KEY (ocs_id) REFERENCES ocs (id),
    CONSTRAINT fk_ocsproc_procedimento FOREIGN KEY (procedimento_id) REFERENCES procedimento_exame (id)
);

CREATE TABLE beneficiario (
    id       BIGINT       NOT NULL AUTO_INCREMENT,
    cpf      VARCHAR(11)  NOT NULL,
    telefone VARCHAR(20),
    idade    INT,
    prec_cp  VARCHAR(20)  NOT NULL,
    nome     VARCHAR(255) NOT NULL,
    senha    VARCHAR(100) NOT NULL,
    CONSTRAINT pk_beneficiario PRIMARY KEY (id),
    CONSTRAINT uk_beneficiario_cpf UNIQUE (cpf),
    CONSTRAINT uk_beneficiario_prec_cp UNIQUE (prec_cp)
);

CREATE TABLE pre_guia (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    status             VARCHAR(20) NOT NULL,
    data_emissao       TIMESTAMP   NOT NULL,
    encaminhamento_url VARCHAR(500),
    beneficiario_id    BIGINT      NOT NULL,
    ocs_id             BIGINT      NOT NULL,
    CONSTRAINT pk_pre_guia PRIMARY KEY (id),
    CONSTRAINT fk_preguia_beneficiario FOREIGN KEY (beneficiario_id) REFERENCES beneficiario (id),
    CONSTRAINT fk_preguia_ocs FOREIGN KEY (ocs_id) REFERENCES ocs (id)
);

-- ATENCAO: esta tabela NAO esta no modelo_logico.txt. Foi adicionada porque a pre-guia
-- precisa guardar "os procedimentos/exames a serem realizados" (US2).
-- Veja docs/DECISOES_PENDENTES.md
CREATE TABLE pre_guia_procedimento (
    pre_guia_id     BIGINT NOT NULL,
    procedimento_id BIGINT NOT NULL,
    CONSTRAINT pk_pre_guia_procedimento PRIMARY KEY (pre_guia_id, procedimento_id),
    CONSTRAINT fk_pgproc_pre_guia FOREIGN KEY (pre_guia_id) REFERENCES pre_guia (id),
    CONSTRAINT fk_pgproc_procedimento FOREIGN KEY (procedimento_id) REFERENCES procedimento_exame (id)
);
