-- T28-BE3 (SCRUM-281): ajustes de integridade do modelo (relatorio 5.3, M2 e M3).

-- M2: o V.Unit da Guia nao e lido nem gravado (regra 3); a coluna sai. O CHECK que a usa sai antes.
ALTER TABLE GuiaProcedimento
    DROP CHECK ck_guiaprocedimento_valor,
    DROP COLUMN valor_unitario;

-- M3: a OCS tem um espelho em construcao por vez (regra 10.9). A coluna gerada so tem valor no espelho
-- EM_CONSTRUCAO; NULL nao conflita no UNIQUE, entao os espelhos ENVIADO nao sao afetados.
-- Se alguma OCS ja tiver dois espelhos em construcao, o ADD CONSTRAINT falha com
-- "Duplicate entry ... for key uk_espelho_ocs_em_construcao", mostrando o id da OCS; corrija antes de rodar de novo.
ALTER TABLE Espelho
    ADD COLUMN ocs_em_construcao BIGINT
        GENERATED ALWAYS AS (CASE WHEN status = 'EM_CONSTRUCAO' THEN ocs_id END) STORED,
    ADD CONSTRAINT uk_espelho_ocs_em_construcao UNIQUE (ocs_em_construcao);
