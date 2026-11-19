-- T16-BE3: validade_ate da Guia passa a ser obrigatoria. SO PODE RODAR QUANDO NENHUMA GUIA ESTIVER SEM VALIDADE
-- (a correcao das Guias antigas, CorrecaoValidadeGuias, preenche a data a partir do PDF guardado).
-- SQL escrito para funcionar em MySQL 8 E em H2 (modo MySQL).

ALTER TABLE Guia
    MODIFY validade_ate DATE NOT NULL;
