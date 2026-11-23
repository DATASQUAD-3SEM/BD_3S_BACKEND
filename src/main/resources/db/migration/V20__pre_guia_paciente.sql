-- T30-BE1 (SCRUM-258): pre-guia com paciente. dependente_id nulo = o paciente e o proprio titular.
-- criada_por e quem criou a pre-guia (pode ser o responsavel externo); nula nas pre-guias antigas.
-- paciente_menor_na_criacao nao entra (decisao do relatorio, M13): quem age vem do tipo do Dependente.
ALTER TABLE PreGuia
    ADD COLUMN dependente_id BIGINT NULL,
    ADD COLUMN criada_por BIGINT NULL,
    ADD CONSTRAINT fk_preguia_dependente FOREIGN KEY (dependente_id) REFERENCES Dependente (id_dependente),
    ADD CONSTRAINT fk_preguia_criada_por FOREIGN KEY (criada_por) REFERENCES Usuario (id_usuario);

CREATE INDEX ix_preguia_dependente ON PreGuia (dependente_id);
