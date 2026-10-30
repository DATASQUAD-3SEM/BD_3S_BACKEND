-- V7: assinatura digital por cadastro (Assinatura), Usuario.assinatura_id e snapshot em AssinaturaGuia.

-- =====================================================================
-- Assinatura: uma linha por cadastro de imagem
-- =====================================================================
CREATE TABLE Assinatura (
                            id_assinatura  BIGINT       NOT NULL AUTO_INCREMENT,
                            usuario_id     BIGINT       NOT NULL,
                            assinatura_url VARCHAR(500) NOT NULL,

                            CONSTRAINT pk_assinatura PRIMARY KEY (id_assinatura),
                            CONSTRAINT uk_assinatura_usuario_assinatura UNIQUE KEY (usuario_id, assinatura_url),
                            CONSTRAINT fk_assinatura_usuario FOREIGN KEY (usuario_id) REFERENCES Usuario (id_usuario)
);

-- =====================================================================
-- Usuario: assinatura_url (V2) passa a ser assinatura_id
-- =====================================================================
ALTER TABLE Usuario
    ADD COLUMN assinatura_id BIGINT NULL;

-- Backfill: cada assinatura_url atual vira uma linha em Assinatura (em bancos vazios, nao faz nada)
INSERT INTO Assinatura (usuario_id, assinatura_url)
SELECT u.id_usuario, u.assinatura_url
FROM Usuario u
WHERE u.assinatura_url IS NOT NULL
  AND TRIM(u.assinatura_url) <> '';

UPDATE Usuario u
    JOIN Assinatura a
ON a.usuario_id = u.id_usuario
    AND a.assinatura_url = u.assinatura_url
    SET u.assinatura_id = a.id_assinatura;

ALTER TABLE Usuario
    ADD CONSTRAINT fk_usuario_assinatura
        FOREIGN KEY (assinatura_id) REFERENCES Assinatura (id_assinatura);

-- A regra da V2 (SETOR_GUIAS e RECEPCAO nao tem assinatura) referenciava assinatura_url.
-- A CHECK precisa sair antes da coluna.
ALTER TABLE Usuario
DROP CHECK ck_usuario_assinatura_url;

ALTER TABLE Usuario
    ADD CONSTRAINT ck_usuario_assinatura_id
        CHECK (perfil NOT IN ('SETOR_GUIAS', 'RECEPCAO') OR assinatura_id IS NULL);

ALTER TABLE Usuario
DROP COLUMN assinatura_url;

-- =====================================================================
-- AssinaturaGuia: snapshot da assinatura usada em cada assinatura da Guia
-- =====================================================================
ALTER TABLE AssinaturaGuia
    ADD COLUMN assinatura_id BIGINT NULL;

-- ATENCAO: o historico da imagem usada em cada assinatura antiga se perdeu (a logica anterior
-- substituia o arquivo). O unico vinculo possivel e a assinatura vigente do usuario na migration.
UPDATE AssinaturaGuia ag
    JOIN Usuario u ON u.id_usuario = ag.usuario_id
    SET ag.assinatura_id = u.assinatura_id
WHERE ag.assinatura_id IS NULL;

-- Falha de proposito se sobrar assinatura de Guia sem Assinatura vinculada.
ALTER TABLE AssinaturaGuia
    MODIFY assinatura_id BIGINT NOT NULL;

ALTER TABLE AssinaturaGuia
    ADD CONSTRAINT fk_assinaturaguia_assinatura
        FOREIGN KEY (assinatura_id) REFERENCES Assinatura (id_assinatura);