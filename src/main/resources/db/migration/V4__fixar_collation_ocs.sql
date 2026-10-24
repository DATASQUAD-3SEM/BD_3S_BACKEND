-- V4: fixa o collation da tabela "Ocs" em utf8mb4_unicode_ci (T2-BE2)
-- A busca GET /ocs?busca= precisa ignorar maiusculas, minusculas e acentos.
-- Quem faz isso e o collation "ci"/"ai" da coluna. Sem fixar aqui, o resultado
-- dependeria do padrao do banco de cada ambiente (volume antigo do Docker,
-- MySQL instalado fora do Docker, etc.).
-- CONVERT TO atualiza a tabela e todas as colunas de texto (nome, tipo, contrato_num).

ALTER TABLE Ocs CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
