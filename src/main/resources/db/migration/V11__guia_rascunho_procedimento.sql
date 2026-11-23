-- T22-BE1: exames e quantidades conferidos pelo Setor de Guias, guardados no rascunho.
-- Substituidos a cada novo PUT do rascunho e copiados para GuiaProcedimento ao concluir (T22-BE2).
-- SQL escrito para funcionar em MySQL 8 E em H2 (modo MySQL).

CREATE TABLE GuiaRascunhoProcedimento (
    pre_guia_id            BIGINT       NOT NULL,
    ordem                  INT          NOT NULL,
    cod_dgp                VARCHAR(10)  NOT NULL,
    descricao_procedimento VARCHAR(500) NOT NULL,
    quantidade_autorizada  INT          NOT NULL,

    CONSTRAINT pk_guia_rascunho_procedimento PRIMARY KEY (pre_guia_id, ordem),
    CONSTRAINT fk_guia_rascunho_procedimento_rascunho
        FOREIGN KEY (pre_guia_id) REFERENCES GuiaRascunho (pre_guia_id),
    CONSTRAINT ck_guia_rascunho_procedimento_cod_dgp CHECK (REGEXP_LIKE(cod_dgp, '^[0-9]+$')),
    CONSTRAINT ck_guia_rascunho_procedimento_quantidade CHECK (quantidade_autorizada > 0)
);
