-- T16-BE3: validade_ate da Guia passa a obrigatoria. So pode rodar quando nenhuma Guia estiver sem validade
-- (a correcao das Guias antigas, CorrecaoValidadeGuias, preenche a data a partir do PDF guardado).
-- SQL escrito para funcionar em MySQL 8 E em H2 (modo MySQL).

ALTER TABLE Guia
    MODIFY validade_ate DATE NOT NULL;
