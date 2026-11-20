ALTER TABLE FaturaEvento DROP CHECK ck_faturaevento_tipo;
ALTER TABLE FaturaEvento
    ADD CONSTRAINT ck_faturaevento_tipo CHECK (
        tipo IN ('PROTOCOLADA', 'ENVIADA_LISURA', 'ENVIADA_OCS', 'CIENTE_OCS', 'CONTESTADA_OCS',
                 'ENVIADA_AUDITORIA', 'VISUALIZADA'));