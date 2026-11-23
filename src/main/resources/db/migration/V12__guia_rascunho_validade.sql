-- T16-BE2: validade ("NAO RECEBER APOS") conferida pelo Setor de Guias, guardada no rascunho.
-- Nullable: rascunhos gravados antes desta migration ficam sem validade (a conclusao recusa com 409, T16-BE3).
-- A UG e a UG Origem nao sao gravadas: so conferidas na requisicao.
-- SQL escrito para funcionar em MySQL 8 E em H2 (modo MySQL).

ALTER TABLE GuiaRascunho
    ADD COLUMN validade_ate DATE NULL;
