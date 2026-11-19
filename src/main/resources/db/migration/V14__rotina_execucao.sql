-- T16-BE3: controle de rotinas de execucao unica. Uma linha por rotina que ja rodou (a correcao de validade
-- das Guias antigas grava aqui ao terminar, e nao roda de novo).
-- SQL escrito para funcionar em MySQL 8 E em H2 (modo MySQL).

CREATE TABLE RotinaExecucao (
    nome           VARCHAR(100) NOT NULL,
    data_execucao  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_rotina_execucao PRIMARY KEY (nome)
);
