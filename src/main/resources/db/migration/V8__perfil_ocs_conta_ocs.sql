-- V8: perfil OCS e ContaOcs (T19-BE0). A conta da OCS se liga a uma linha de Ocs como o Beneficiario
-- se liga a Usuario: tabela de especializacao, uma conta por OCS (ocs_id UNIQUE).

ALTER TABLE Usuario DROP CHECK ck_usuario_perfil;
ALTER TABLE Usuario
    ADD CONSTRAINT ck_usuario_perfil
        CHECK (perfil IN ('BENEFICIARIO', 'MEDICO', 'CHEFE_FUSEX', 'SETOR_GUIAS', 'RECEPCAO', 'OCS'));

-- Conta OCS nao tem assinatura, como SETOR_GUIAS e RECEPCAO
ALTER TABLE Usuario DROP CHECK ck_usuario_assinatura_id;
ALTER TABLE Usuario
    ADD CONSTRAINT ck_usuario_assinatura_id
        CHECK (perfil NOT IN ('SETOR_GUIAS', 'RECEPCAO', 'OCS') OR assinatura_id IS NULL);

-- =====================================================================
-- ContaOcs: especializacao de Usuario para o perfil OCS (PK = FK, como Beneficiario)
-- =====================================================================
CREATE TABLE ContaOcs (
                          usuario_id BIGINT NOT NULL,
                          ocs_id     BIGINT NOT NULL,

                          CONSTRAINT pk_conta_ocs PRIMARY KEY (usuario_id),
                          CONSTRAINT fk_conta_ocs_usuario FOREIGN KEY (usuario_id) REFERENCES Usuario (id_usuario),
                          CONSTRAINT fk_conta_ocs_ocs FOREIGN KEY (ocs_id) REFERENCES Ocs (id_ocs),
                          CONSTRAINT uk_conta_ocs_ocs UNIQUE (ocs_id)
);
