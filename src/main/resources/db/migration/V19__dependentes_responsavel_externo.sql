-- T29-BE1 (SCRUM-250): dependentes e responsavel externo, conforme modelo_logico.txt secao 2.7 e
-- regras_negocio_e_fluxo.txt item 13. Decisao D2 e regra 13: responsavel externo tem conta propria pelo CPF
-- e assinatura, SEM Prec CP. Um responsavel por menor (Dependente.responsavel_id).
-- Migracao nova: a V15 de dependentes da branch antiga foi descartada.

-- Perfil RESPONSAVEL_EXTERNO (19 caracteres): coluna passa a VARCHAR(20).
-- Tem assinatura (assina as sessoes dos menores), entao nao entra em ck_usuario_assinatura_id.
ALTER TABLE Usuario MODIFY perfil VARCHAR(20) NOT NULL;
ALTER TABLE Usuario DROP CHECK ck_usuario_perfil;
ALTER TABLE Usuario
    ADD CONSTRAINT ck_usuario_perfil
        CHECK (perfil IN ('BENEFICIARIO', 'MEDICO', 'CHEFE_FUSEX', 'SETOR_GUIAS', 'RECEPCAO', 'OCS',
                          'SETOR_FATURA', 'RASTREABILIDADE', 'AUDITORIA', 'RESPONSAVEL_EXTERNO'));

-- ResponsavelExterno: especializacao de Usuario (PK = FK). Pessoa externa, sem Prec CP.
CREATE TABLE ResponsavelExterno (
    usuario_id BIGINT       NOT NULL,
    nome       VARCHAR(150) NOT NULL,
    telefone   VARCHAR(11)  NULL,

    CONSTRAINT pk_responsavelexterno PRIMARY KEY (usuario_id),
    CONSTRAINT fk_responsavelexterno_usuario FOREIGN KEY (usuario_id) REFERENCES Usuario (id_usuario),
    CONSTRAINT ck_responsavelexterno_nome CHECK (CHAR_LENGTH(TRIM(nome)) > 0),
    CONSTRAINT ck_responsavelexterno_telefone
        CHECK (telefone IS NULL OR REGEXP_LIKE(telefone, '^[1-9][1-9]9[0-9]{8}$'))
);

-- Dependente: sempre de um titular (Beneficiario), cadastrado pelo chefe do FUSEX.
--   MENOR_TITULAR:     sem conta; o titular age por ele (usuario_id e responsavel_id nulos).
--   MENOR_RESPONSAVEL: sem conta; o responsavel externo age por ele (responsavel_id preenchido).
--   MAIOR:             conta propria (usuario_id, perfil BENEFICIARIO); responsavel_id nulo.
-- vigencia_fim: menor sempre tem data final; maior nula = vitalicia. Nada e encerrado por rotina.
CREATE TABLE Dependente (
    id_dependente   BIGINT       NOT NULL AUTO_INCREMENT,
    titular_id      BIGINT       NOT NULL,
    usuario_id      BIGINT       NULL,
    responsavel_id  BIGINT       NULL,
    nome            VARCHAR(150) NOT NULL,
    data_nascimento DATE         NOT NULL,
    cpf             VARCHAR(11)  NOT NULL,
    telefone        VARCHAR(11)  NULL,
    prec_cp         VARCHAR(11)  NOT NULL,
    vinculo         VARCHAR(50)  NULL,
    tipo            VARCHAR(17)  NOT NULL,
    vigencia_fim    DATE         NULL,

    CONSTRAINT pk_dependente PRIMARY KEY (id_dependente),
    CONSTRAINT fk_dependente_titular FOREIGN KEY (titular_id) REFERENCES Beneficiario (usuario_id),
    CONSTRAINT fk_dependente_usuario FOREIGN KEY (usuario_id) REFERENCES Usuario (id_usuario),
    CONSTRAINT fk_dependente_responsavel FOREIGN KEY (responsavel_id) REFERENCES Usuario (id_usuario),
    CONSTRAINT uk_dependente_usuario UNIQUE (usuario_id),
    CONSTRAINT uk_dependente_cpf UNIQUE (cpf),
    CONSTRAINT uk_dependente_prec_cp UNIQUE (prec_cp),
    CONSTRAINT ck_dependente_nome CHECK (CHAR_LENGTH(TRIM(nome)) > 0),
    CONSTRAINT ck_dependente_cpf CHECK (REGEXP_LIKE(cpf, '^[0-9]{11}$')),
    CONSTRAINT ck_dependente_prec_cp CHECK (REGEXP_LIKE(prec_cp, '^[0-9]{11}$')),
    CONSTRAINT ck_dependente_telefone CHECK (telefone IS NULL OR REGEXP_LIKE(telefone, '^[1-9][1-9]9[0-9]{8}$')),
    CONSTRAINT ck_dependente_tipo CHECK (
        (tipo = 'MENOR_TITULAR' AND usuario_id IS NULL AND responsavel_id IS NULL AND vigencia_fim IS NOT NULL) OR
        (tipo = 'MENOR_RESPONSAVEL' AND usuario_id IS NULL AND responsavel_id IS NOT NULL AND vigencia_fim IS NOT NULL) OR
        (tipo = 'MAIOR' AND usuario_id IS NOT NULL AND responsavel_id IS NULL))
);
