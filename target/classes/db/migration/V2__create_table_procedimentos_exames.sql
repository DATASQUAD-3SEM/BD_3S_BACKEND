-- Tabela de Procedimentos/Exames (catálogo TUSS)
-- Fonte dos dados: rol_tuss_rol_simplificado...xls (Tabela 22 - Terminologia TUSS)

CREATE TABLE procedimentos_exames (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    codigo_tuss         VARCHAR(20)     NOT NULL,
    descricao           VARCHAR(500)    NOT NULL,
    subgrupo            VARCHAR(255),
    grupo               VARCHAR(255),
    capitulo            VARCHAR(255),
    ativo               BOOLEAN         NOT NULL DEFAULT TRUE,
    criado_em           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_procedimento_codigo_tuss UNIQUE (codigo_tuss)
);

CREATE INDEX idx_procedimento_descricao ON procedimentos_exames (descricao);
CREATE INDEX idx_procedimento_grupo ON procedimentos_exames (grupo);
