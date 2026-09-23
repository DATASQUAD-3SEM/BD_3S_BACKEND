-- Tabela de OCS (Organizações Civis de Saúde) / PSA
-- Fonte dos dados: TABELA_DE_PARÂMETROS_DE_PREÇOS_-_UG-FUSEX.xls (Tabela 1 de cada aba)

CREATE TABLE ocs (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome                VARCHAR(255)    NOT NULL,
    tipo                VARCHAR(10)     NOT NULL,               -- 'OCS' ou 'PSA'
    contrato_numero     VARCHAR(50)     NOT NULL,
    inicio_vigencia     DATE            NOT NULL,
    termino_vigencia    DATE            NOT NULL,
    ativa               BOOLEAN         NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_ocs_tipo CHECK (tipo IN ('OCS', 'PSA'))
);

CREATE INDEX idx_ocs_nome ON ocs (nome);
CREATE INDEX idx_ocs_ativa ON ocs (ativa);
