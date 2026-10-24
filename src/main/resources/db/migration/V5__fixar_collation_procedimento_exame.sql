-- V5: fixa o collation da tabela "ProcedimentoExame" em utf8mb4_unicode_ci (T2-BE3)
-- A busca GET /medicos/procedimentos?busca= precisa ignorar maiusculas, minusculas e acentos,
-- igual a busca de OCS (V4). Sem fixar aqui, o resultado dependeria do padrao do banco de cada ambiente.
-- CONVERT TO atualiza a tabela e todas as colunas de texto (codigo_tuss, terminologia_procedimento, etc.).

ALTER TABLE ProcedimentoExame CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
