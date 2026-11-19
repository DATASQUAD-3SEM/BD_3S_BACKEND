-- T30-BE1: pre-guia com paciente. dependente_id nulo = o paciente e o proprio titular (como antes).
-- paciente_menor_na_criacao decide quem age na pre-guia e na Guia (T30-BE3): verdadeiro so com dependente.
-- criada_por e quem criou a pre-guia (pode ser o responsavel externo); nula nas pre-guias antigas.
-- Pre-guias existentes ficam com dependente_id nulo: nenhuma outra alteracao.
-- SQL escrito para MySQL 8.

ALTER TABLE PreGuia
    ADD COLUMN dependente_id BIGINT NULL,
    ADD COLUMN paciente_menor_na_criacao BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN criada_por BIGINT NULL,
    ADD CONSTRAINT fk_preguia_dependente FOREIGN KEY (dependente_id) REFERENCES Dependente (id_dependente),
    ADD CONSTRAINT fk_preguia_criada_por FOREIGN KEY (criada_por) REFERENCES Usuario (id_usuario),
    ADD CONSTRAINT ck_preguia_paciente_menor CHECK (paciente_menor_na_criacao = FALSE OR dependente_id IS NOT NULL);

CREATE INDEX ix_preguia_dependente ON PreGuia (dependente_id);
