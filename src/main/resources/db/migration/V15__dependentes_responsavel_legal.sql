-- T29-BE1: dependentes, responsaveis legais e o perfil RESPONSAVEL_EXTERNO.
-- Dependente nao tem conta obrigatoria (usuario_id so quando tem conta propria). ResponsavelLegal aponta para
-- Usuario, nunca para Dependente: perder o status de dependente nao encerra o vinculo de responsavel.
-- SQL escrito para MySQL 8 (usa DROP CHECK, como a V8).

-- Perfil RESPONSAVEL_EXTERNO (18 caracteres): a coluna passa de VARCHAR(15) para VARCHAR(20).
-- Sem assinatura na criacao: a CHECK de assinatura nao inclui o perfil, entao ele cadastra no primeiro acesso.
ALTER TABLE Usuario MODIFY perfil VARCHAR(20) NOT NULL;
ALTER TABLE Usuario DROP CHECK ck_usuario_perfil;
ALTER TABLE Usuario
    ADD CONSTRAINT ck_usuario_perfil
        CHECK (perfil IN ('BENEFICIARIO', 'MEDICO', 'CHEFE_FUSEX', 'SETOR_GUIAS', 'RECEPCAO', 'OCS',
                          'RESPONSAVEL_EXTERNO'));

-- ---------------------------------------------------------------------
-- Dependente: pertence a um titular (Beneficiario), cadastrado pelo chefe do FUSEX.
-- ---------------------------------------------------------------------
CREATE TABLE Dependente (
    id_dependente     BIGINT       NOT NULL AUTO_INCREMENT,
    titular_id        BIGINT       NOT NULL,
    usuario_id        BIGINT       NULL,
    nome              VARCHAR(150) NOT NULL,
    data_nascimento   DATE         NOT NULL,
    cpf               VARCHAR(11)  NOT NULL,
    telefone          VARCHAR(11)  NULL,
    prec_cp           VARCHAR(11)  NOT NULL,
    vinculo           VARCHAR(100) NULL,
    tipo_vigencia     VARCHAR(12)  NOT NULL,
    vigencia_fim      DATE         NULL,
    situacao          VARCHAR(10)  NOT NULL DEFAULT 'ATIVO',
    data_encerramento DATETIME     NULL,

    CONSTRAINT pk_dependente PRIMARY KEY (id_dependente),
    CONSTRAINT fk_dependente_titular FOREIGN KEY (titular_id) REFERENCES Beneficiario (usuario_id),
    CONSTRAINT fk_dependente_usuario FOREIGN KEY (usuario_id) REFERENCES Usuario (id_usuario),
    CONSTRAINT uk_dependente_usuario UNIQUE (usuario_id),
    CONSTRAINT uk_dependente_prec_cp UNIQUE (prec_cp),
    CONSTRAINT ck_dependente_nome CHECK (CHAR_LENGTH(TRIM(nome)) > 0),
    CONSTRAINT ck_dependente_cpf CHECK (REGEXP_LIKE(cpf, '^[0-9]{11}$')),
    CONSTRAINT ck_dependente_prec_cp CHECK (REGEXP_LIKE(prec_cp, '^[0-9]{11}$')),
    CONSTRAINT ck_dependente_tipo_vigencia CHECK (tipo_vigencia IN ('VITALICIA', 'DETERMINADA')),
    CONSTRAINT ck_dependente_vigencia CHECK (
        (tipo_vigencia = 'DETERMINADA' AND vigencia_fim IS NOT NULL) OR
        (tipo_vigencia = 'VITALICIA' AND vigencia_fim IS NULL)),
    CONSTRAINT ck_dependente_situacao CHECK (situacao IN ('ATIVO', 'ENCERRADO')),
    CONSTRAINT ck_dependente_encerramento CHECK (
        (situacao = 'ENCERRADO' AND data_encerramento IS NOT NULL) OR
        (situacao = 'ATIVO' AND data_encerramento IS NULL))
);

-- ---------------------------------------------------------------------
-- ResponsavelLegal: quem acompanha um dependente menor. Historico: remover grava data_fim, nunca apaga a linha.
-- ---------------------------------------------------------------------
CREATE TABLE ResponsavelLegal (
    id_responsavel_legal BIGINT NOT NULL AUTO_INCREMENT,
    dependente_id        BIGINT NOT NULL,
    usuario_id           BIGINT NOT NULL,
    data_inicio          DATE   NOT NULL,
    data_fim             DATE   NULL,

    CONSTRAINT pk_responsavel_legal PRIMARY KEY (id_responsavel_legal),
    CONSTRAINT fk_responsavel_legal_dependente FOREIGN KEY (dependente_id) REFERENCES Dependente (id_dependente),
    CONSTRAINT fk_responsavel_legal_usuario FOREIGN KEY (usuario_id) REFERENCES Usuario (id_usuario),
    CONSTRAINT ck_responsavel_legal_periodo CHECK (data_fim IS NULL OR data_fim >= data_inicio)
);

-- "Quem acompanha este menor hoje": consulta por dependente com data_fim nula
CREATE INDEX ix_responsavel_legal_dependente_fim ON ResponsavelLegal (dependente_id, data_fim);
