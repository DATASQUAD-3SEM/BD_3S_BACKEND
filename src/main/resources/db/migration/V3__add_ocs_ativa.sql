-- V3: inserido coluna "ativo" dentro da tabela "Ocs"
-- Para cobrir feat/SCRUM-85, é necessário ter essa coluna

ALTER TABLE Ocs ADD COLUMN ativo BOOLEAN NOT NULL DEFAULT TRUE;