-- V2: adequacao ao modelo logico 3.2 (Sprint 2)
-- Tabelas: PascalCase. Colunas: snake_case. PK: id_<tabela>, exceto especializacoes (PK = FK usuario_id).
-- ATENCAO: remove colunas de beneficiario (cpf, idade, senha). Aplicar apenas em banco de desenvolvimento.


-- =====================================================================
-- Usuario (nova)
-- =====================================================================
CREATE TABLE Usuario (
    id_usuario     BIGINT       NOT NULL AUTO_INCREMENT,
    login          VARCHAR(50)  NOT NULL,
    senha_hash     VARCHAR(255) NOT NULL,
    perfil         VARCHAR(15)  NOT NULL,
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE,
    assinatura_url VARCHAR(500),

    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uk_usuario_login UNIQUE (login),
    CONSTRAINT ck_usuario_login CHECK (CHAR_LENGTH(login) >= 3),
    CONSTRAINT ck_usuario_perfil
     CHECK (perfil IN ('BENEFICIARIO', 'MEDICO', 'CHEFE_FUSEX', 'SETOR_GUIAS', 'RECEPCAO')),
    CONSTRAINT ck_usuario_assinatura_url
     CHECK (perfil NOT IN ('SETOR_GUIAS', 'RECEPCAO') OR assinatura_url IS NULL)
);


-- =====================================================================
-- Beneficiario (era beneficiario, PK passa a ser usuario_id)
-- =====================================================================
-- A FK da V1 impede alterar a coluna referenciada: remove antes, recria no fim.
ALTER TABLE pre_guia DROP FOREIGN KEY fk_preguia_beneficiario;

RENAME TABLE beneficiario TO Beneficiario_tmp;
RENAME TABLE Beneficiario_tmp TO Beneficiario;

ALTER TABLE Beneficiario RENAME COLUMN id TO usuario_id;
ALTER TABLE Beneficiario MODIFY usuario_id BIGINT NOT NULL;
ALTER TABLE Beneficiario ADD CONSTRAINT fk_beneficiario_usuario
    FOREIGN KEY (usuario_id) REFERENCES Usuario (id_usuario);

ALTER TABLE Beneficiario DROP COLUMN cpf;
ALTER TABLE Beneficiario DROP COLUMN idade;
ALTER TABLE Beneficiario DROP COLUMN senha;

ALTER TABLE Beneficiario
    MODIFY nome VARCHAR(150) NOT NULL,
    ADD CONSTRAINT ck_beneficiario_nome CHECK (CHAR_LENGTH(TRIM(nome)) > 0);

ALTER TABLE Beneficiario
    MODIFY prec_cp VARCHAR(11) NOT NULL,
    ADD CONSTRAINT ck_beneficiario_prec_cp CHECK (REGEXP_LIKE(prec_cp, '^[0-9]{11}$'));

ALTER TABLE Beneficiario
    MODIFY telefone VARCHAR(11) NOT NULL,
    ADD CONSTRAINT ck_beneficiario_telefone CHECK (REGEXP_LIKE(telefone, '^[1-9][1-9]9[0-9]{8}$'));

ALTER TABLE Beneficiario ADD COLUMN data_nascimento DATE NOT NULL;

-- Recria a FK da pre-guia, agora apontando para usuario_id (regra do modelo 3.2)
ALTER TABLE pre_guia ADD CONSTRAINT fk_preguia_beneficiario
    FOREIGN KEY (beneficiario_id) REFERENCES Beneficiario (usuario_id);


-- =====================================================================
-- MedicoFusex (nova; PK = FK usuario_id)
-- =====================================================================
CREATE TABLE MedicoFusex (
    usuario_id BIGINT       NOT NULL,
    nome       VARCHAR(150) NOT NULL,
    prec_cp    VARCHAR(11)     NOT NULL,
    patente    VARCHAR(50)  NOT NULL,
    crm        VARCHAR(20)  NOT NULL,

    PRIMARY KEY (usuario_id),
    CONSTRAINT fk_medicofusex_usuario FOREIGN KEY (usuario_id) REFERENCES Usuario (id_usuario),
    CONSTRAINT uk_medicofusex_prec_cp UNIQUE (prec_cp),
    CONSTRAINT uk_medicofusex_crm UNIQUE (crm),
    CONSTRAINT ck_medicofusex_nome CHECK (CHAR_LENGTH(TRIM(nome)) > 0),
    CONSTRAINT ck_medicofusex_prec_cp CHECK (REGEXP_LIKE(prec_cp, '^[0-9]{11}$')),
    CONSTRAINT ck_medicofusex_crm CHECK (REGEXP_LIKE(crm, '^[0-9]+-[A-Z]{2}$', 'c'))
);


-- =====================================================================
-- ChefeFusex (nova; PK = FK usuario_id)
-- =====================================================================
CREATE TABLE ChefeFusex (
    usuario_id BIGINT       NOT NULL,
    nome       VARCHAR(150) NOT NULL,
    patente    VARCHAR(50)  NOT NULL,

    PRIMARY KEY (usuario_id),
    CONSTRAINT fk_chefefusex_usuario FOREIGN KEY (usuario_id) REFERENCES Usuario (id_usuario),
    CONSTRAINT ck_chefefusex_nome CHECK (CHAR_LENGTH(TRIM(nome)) > 0)
);


-- =====================================================================
-- Ocs (era ocs)
-- =====================================================================
RENAME TABLE ocs TO Ocs_tmp;
RENAME TABLE Ocs_tmp TO Ocs;
ALTER TABLE Ocs RENAME COLUMN id TO id_ocs;

ALTER TABLE Ocs
    DROP COLUMN inicio_vigencia,
    DROP COLUMN dias_para_vencimento,
    DROP COLUMN termino_vigencia;

ALTER TABLE Ocs
    MODIFY nome VARCHAR(200) NOT NULL,
    MODIFY tipo VARCHAR(3) NOT NULL,
    ADD CONSTRAINT ck_ocs_tipo CHECK (tipo IN ('OCS', 'PSA'));


-- =====================================================================
-- ProcedimentoExame (era procedimento_exame)
-- =====================================================================
RENAME TABLE procedimento_exame TO ProcedimentoExame;
ALTER TABLE ProcedimentoExame RENAME COLUMN id TO id_procedimento_exame;
ALTER TABLE ProcedimentoExame RENAME COLUMN terminologia_procedimento_evento TO terminologia_procedimento;

ALTER TABLE ProcedimentoExame
    DROP COLUMN grupo,
    DROP COLUMN capitulo;

ALTER TABLE ProcedimentoExame
    MODIFY codigo_tuss VARCHAR(10) NOT NULL,
    MODIFY subgrupo VARCHAR(150),
    MODIFY rol_ans VARCHAR(3) NOT NULL,
    ADD CONSTRAINT uk_procedimentoexame_codigo_tuss UNIQUE (codigo_tuss),
    ADD CONSTRAINT ck_procedimentoexame_rol_ans CHECK (rol_ans IN ('SIM', 'NAO'));


-- =====================================================================
-- OcsProcedimento (era ocs_procedimento; FKs renomeadas no fim)
-- =====================================================================
RENAME TABLE ocs_procedimento TO OcsProcedimento;


-- =====================================================================
-- PreGuia (era pre_guia)
-- =====================================================================
RENAME TABLE pre_guia TO PreGuia;
ALTER TABLE PreGuia RENAME COLUMN id TO id_pre_guia;
ALTER TABLE PreGuia RENAME COLUMN data_emissao TO data_criacao;

ALTER TABLE PreGuia
    MODIFY status VARCHAR(12) NOT NULL DEFAULT 'PENDENTE',
    MODIFY data_criacao DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD CONSTRAINT ck_preguia_status
        CHECK (status IN ('PENDENTE', 'EM_ANALISE', 'ATENCAO', 'CONCLUIDA'));

ALTER TABLE PreGuia ADD COLUMN medico_id BIGINT NULL;

ALTER TABLE PreGuia
    ADD CONSTRAINT fk_preguia_medicofusex FOREIGN KEY (medico_id) REFERENCES MedicoFusex (usuario_id),
    ADD CONSTRAINT ck_preguia_encaminhamento_url
        CHECK (encaminhamento_url IS NOT NULL OR medico_id IS NOT NULL);


-- =====================================================================
-- PreGuiaProcedimento (era pre_guia_procedimento; FKs renomeadas no fim)
-- =====================================================================
RENAME TABLE pre_guia_procedimento TO PreGuiaProcedimento;


-- =====================================================================
-- PreGuiaHistorico (nova; so recebe INSERT)
-- =====================================================================
CREATE TABLE PreGuiaHistorico (
    id_pre_guia_historico BIGINT      NOT NULL AUTO_INCREMENT,
    pre_guia_id           BIGINT      NOT NULL,
    status                VARCHAR(12) NOT NULL,
    observacao            VARCHAR(500),
    encaminhamento_url    VARCHAR(500),
    usuario_id            BIGINT      NOT NULL,
    data_hora             DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_preguiahistorico PRIMARY KEY (id_pre_guia_historico),
    CONSTRAINT fk_preguiahistorico_preguia FOREIGN KEY (pre_guia_id) REFERENCES PreGuia (id_pre_guia),
    CONSTRAINT fk_preguiahistorico_usuario FOREIGN KEY (usuario_id) REFERENCES Usuario (id_usuario),
    CONSTRAINT ck_preguiahistorico_status
      CHECK (status IN ('PENDENTE', 'EM_ANALISE', 'ATENCAO', 'CONCLUIDA')),
    CONSTRAINT ck_preguiahistorico_observacao
      CHECK (status <> 'ATENCAO' OR (observacao IS NOT NULL AND TRIM(observacao) <> ''))
);


-- =====================================================================
-- Guia (nova)
-- =====================================================================
CREATE TABLE Guia (
    id_guia       BIGINT       NOT NULL AUTO_INCREMENT,
    pre_guia_id   BIGINT       NOT NULL,
    numero        VARCHAR(20)  NOT NULL,
    arquivo_url   VARCHAR(500) NOT NULL,
    data_insercao DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    inserida_por  BIGINT       NOT NULL,

    CONSTRAINT pk_guia PRIMARY KEY (id_guia),
    CONSTRAINT fk_guia_preguia FOREIGN KEY (pre_guia_id) REFERENCES PreGuia (id_pre_guia),
    CONSTRAINT fk_guia_usuario FOREIGN KEY (inserida_por) REFERENCES Usuario (id_usuario),
    CONSTRAINT uk_guia_pre_guia_id UNIQUE (pre_guia_id),
    CONSTRAINT uk_guia_numero UNIQUE (numero)
);


-- =====================================================================
-- AssinaturaGuia (nova)
-- =====================================================================
CREATE TABLE AssinaturaGuia (
    id_assinatura_guia BIGINT     NOT NULL AUTO_INCREMENT,
    guia_id            BIGINT     NOT NULL,
    papel              VARCHAR(6) NOT NULL,
    usuario_id         BIGINT     NOT NULL,
    data_assinatura    DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_assinaturaguia PRIMARY KEY (id_assinatura_guia),
    CONSTRAINT fk_assinaturaguia_guia FOREIGN KEY (guia_id) REFERENCES Guia (id_guia),
    CONSTRAINT fk_assinaturaguia_usuario FOREIGN KEY (usuario_id) REFERENCES Usuario (id_usuario),
    CONSTRAINT uk_assinaturaguia_guia_id_papel_unico UNIQUE (guia_id, papel),
    CONSTRAINT ck_assinaturaguia_papel CHECK (papel IN ('MEDICO', 'CHEFE'))
);


-- =====================================================================
-- Renomear as FKs das tabelas associativas da V1
-- O MySQL nao renomeia constraint de FK: remove e recria com o novo nome.
-- As colunas e tabelas referenciadas ja foram renomeadas acima.
-- =====================================================================
ALTER TABLE OcsProcedimento
    DROP FOREIGN KEY fk_ocsproc_ocs,
    DROP FOREIGN KEY fk_ocsproc_procedimento;

ALTER TABLE OcsProcedimento
    ADD CONSTRAINT fk_ocsprocedimento_ocs
        FOREIGN KEY (ocs_id) REFERENCES Ocs (id_ocs),
    ADD CONSTRAINT fk_ocsprocedimento_procedimentoexame
        FOREIGN KEY (procedimento_id) REFERENCES ProcedimentoExame (id_procedimento_exame);

ALTER TABLE PreGuiaProcedimento
    DROP FOREIGN KEY fk_pgproc_pre_guia,
    DROP FOREIGN KEY fk_pgproc_procedimento;

ALTER TABLE PreGuiaProcedimento
    ADD CONSTRAINT fk_preguiaprocedimento_preguia
        FOREIGN KEY (pre_guia_id) REFERENCES PreGuia (id_pre_guia),
    ADD CONSTRAINT fk_preguiaprocedimento_procedimentoexame
        FOREIGN KEY (procedimento_id) REFERENCES ProcedimentoExame (id_procedimento_exame);