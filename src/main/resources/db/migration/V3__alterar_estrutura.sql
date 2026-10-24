-- Adiciona campos para o fluxo de correção
ALTER TABLE pre_guia ADD COLUMN numero VARCHAR(20) UNIQUE;
ALTER TABLE pre_guia ADD COLUMN origem VARCHAR(20) NOT NULL DEFAULT 'BENEFICIARIO';
ALTER TABLE pre_guia ADD COLUMN observacao TEXT;
ALTER TABLE pre_guia ADD COLUMN data_registro_observacao TIMESTAMP;
ALTER TABLE pre_guia ADD COLUMN encaminhamento_nome VARCHAR(255);
ALTER TABLE pre_guia ADD COLUMN encaminhamento_tipo VARCHAR(50);
ALTER TABLE pre_guia ADD COLUMN data_envio_encaminhamento TIMESTAMP;