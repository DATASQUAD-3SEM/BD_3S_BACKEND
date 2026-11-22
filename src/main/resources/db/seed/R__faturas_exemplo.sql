-- ---------------------------------------------------------------------
-- T28-BE2 (SCRUM-249): faturas de exemplo para testar T24 a T27 (e os versos da T32) a mao.
-- Uma fatura por situacao do fluxo da OCS Laboratorio Exemplo (conta ocs.laboratorio), com os eventos e as rodadas
-- da OCS (EspelhoItemRodadaOcs). Nao entra em producao. Idempotente: roda de novo sem duplicar.
-- ---------------------------------------------------------------------
SET @ocs = (SELECT id_ocs FROM Ocs WHERE contrato_num = 'EXEMPLO-001' AND nome = 'Laboratorio Exemplo');
SET @benef = (SELECT b.usuario_id FROM Beneficiario b JOIN Usuario u ON u.id_usuario = b.usuario_id WHERE u.login = '52998224725');
SET @ocsu = (SELECT id_usuario FROM Usuario WHERE login = 'ocs.laboratorio');
SET @setor = (SELECT id_usuario FROM Usuario WHERE login = 'setor.fatura');
SET @rast = (SELECT id_usuario FROM Usuario WHERE login = 'rastreabilidade');
SET @aud = (SELECT id_usuario FROM Usuario WHERE login = 'auditoria');
SET @guias = (SELECT id_usuario FROM Usuario WHERE login = 'setor.guias');

-- Preco da tabela usado pela linha de origem TABELA (o mesmo papel da carga definitiva).
INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT @ocs, '40304361', 'Hemograma completo', 18.00 FROM DUAL
WHERE @ocs IS NOT NULL AND NOT EXISTS (SELECT 1 FROM OcsPreco WHERE ocs_id = @ocs AND cod_dgp = '40304361');

-- Fatura: Sem nº de fatura (espelho recebido)
INSERT INTO Espelho (ocs_id, status, data_criacao, criado_por, data_envio, enviado_por, situacao_fatura, numero_fatura, protocolada_por)
SELECT @ocs, 'ENVIADO', '2026-10-01 08:00:01', @ocsu, '2026-10-02 09:00:00', @ocsu, 'RECEBIDO', NULL, NULL FROM DUAL
WHERE @ocs IS NOT NULL AND NOT EXISTS (SELECT 1 FROM Espelho WHERE ocs_id = @ocs AND data_criacao = '2026-10-01 08:00:01');
SET @esp = (SELECT id_espelho FROM Espelho WHERE ocs_id = @ocs AND data_criacao = '2026-10-01 08:00:01');
INSERT INTO PreGuia (status, data_criacao, encaminhamento_url, beneficiario_id, ocs_id)
SELECT 'CONCLUIDA', '2026-09-20 09:00:00', 'seed/faturas/900000101.pdf', @benef, @ocs FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM PreGuia WHERE encaminhamento_url = 'seed/faturas/900000101.pdf');
INSERT INTO Guia (pre_guia_id, numero, arquivo_url, data_insercao, inserida_por, validade_ate, data_envio, enviada_por, situacao_execucao, espelho_id)
SELECT p.id_pre_guia, '900000101', 'seed/faturas/900000101.pdf', '2026-09-21 09:00:00', @guias, '2099-12-31', '2026-09-22 09:00:00', @benef, 'REALIZADA', @esp
FROM PreGuia p WHERE p.encaminhamento_url = 'seed/faturas/900000101.pdf' AND NOT EXISTS (SELECT 1 FROM Guia WHERE numero = '900000101');
INSERT INTO GuiaProcedimento (guia_id, cod_dgp, descricao_procedimento, quantidade_autorizada)
SELECT g.id_guia, '40304361', 'Hemograma completo', 1 FROM Guia g WHERE g.numero = '900000101'
  AND NOT EXISTS (SELECT 1 FROM GuiaProcedimento gp WHERE gp.guia_id = g.id_guia AND gp.cod_dgp = '40304361');
INSERT INTO EspelhoItem (espelho_id, guia_procedimento_id, quantidade_sessoes, valor_sessao, valor_contrato_sessao, origem_contrato, observacao, decisao_ocs, motivo_contestacao_ocs)
SELECT @esp, gp.id_guia_procedimento, 1, 20.00, NULL, NULL, NULL, NULL, NULL
FROM GuiaProcedimento gp JOIN Guia g ON g.id_guia = gp.guia_id WHERE g.numero = '900000101' AND gp.cod_dgp = '40304361'
  AND NOT EXISTS (SELECT 1 FROM EspelhoItem ei WHERE ei.guia_procedimento_id = gp.id_guia_procedimento);

-- Fatura: Protocolada (901/2026)
INSERT INTO Espelho (ocs_id, status, data_criacao, criado_por, data_envio, enviado_por, situacao_fatura, numero_fatura, protocolada_por)
SELECT @ocs, 'ENVIADO', '2026-10-01 08:00:02', @ocsu, '2026-10-02 09:00:00', @ocsu, 'PROTOCOLADA', '901/2026', @setor FROM DUAL
WHERE @ocs IS NOT NULL AND NOT EXISTS (SELECT 1 FROM Espelho WHERE ocs_id = @ocs AND data_criacao = '2026-10-01 08:00:02');
SET @esp = (SELECT id_espelho FROM Espelho WHERE ocs_id = @ocs AND data_criacao = '2026-10-01 08:00:02');
INSERT INTO PreGuia (status, data_criacao, encaminhamento_url, beneficiario_id, ocs_id)
SELECT 'CONCLUIDA', '2026-09-20 09:00:00', 'seed/faturas/900000102.pdf', @benef, @ocs FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM PreGuia WHERE encaminhamento_url = 'seed/faturas/900000102.pdf');
INSERT INTO Guia (pre_guia_id, numero, arquivo_url, data_insercao, inserida_por, validade_ate, data_envio, enviada_por, situacao_execucao, espelho_id)
SELECT p.id_pre_guia, '900000102', 'seed/faturas/900000102.pdf', '2026-09-21 09:00:00', @guias, '2099-12-31', '2026-09-22 09:00:00', @benef, 'REALIZADA', @esp
FROM PreGuia p WHERE p.encaminhamento_url = 'seed/faturas/900000102.pdf' AND NOT EXISTS (SELECT 1 FROM Guia WHERE numero = '900000102');
INSERT INTO GuiaProcedimento (guia_id, cod_dgp, descricao_procedimento, quantidade_autorizada)
SELECT g.id_guia, '40301630', 'Glicose', 2 FROM Guia g WHERE g.numero = '900000102'
  AND NOT EXISTS (SELECT 1 FROM GuiaProcedimento gp WHERE gp.guia_id = g.id_guia AND gp.cod_dgp = '40301630');
INSERT INTO EspelhoItem (espelho_id, guia_procedimento_id, quantidade_sessoes, valor_sessao, valor_contrato_sessao, origem_contrato, observacao, decisao_ocs, motivo_contestacao_ocs)
SELECT @esp, gp.id_guia_procedimento, 2, 12.00, NULL, NULL, NULL, NULL, NULL
FROM GuiaProcedimento gp JOIN Guia g ON g.id_guia = gp.guia_id WHERE g.numero = '900000102' AND gp.cod_dgp = '40301630'
  AND NOT EXISTS (SELECT 1 FROM EspelhoItem ei WHERE ei.guia_procedimento_id = gp.id_guia_procedimento);
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'PROTOCOLADA', @setor, 'Sgt Ana Silva', '2026-10-03 09:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'PROTOCOLADA');

-- Fatura: Em lisura, com linha de preço da tabela (carga) (902/2026)
INSERT INTO Espelho (ocs_id, status, data_criacao, criado_por, data_envio, enviado_por, situacao_fatura, numero_fatura, protocolada_por)
SELECT @ocs, 'ENVIADO', '2026-10-01 08:00:03', @ocsu, '2026-10-02 09:00:00', @ocsu, 'EM_LISURA', '902/2026', @setor FROM DUAL
WHERE @ocs IS NOT NULL AND NOT EXISTS (SELECT 1 FROM Espelho WHERE ocs_id = @ocs AND data_criacao = '2026-10-01 08:00:03');
SET @esp = (SELECT id_espelho FROM Espelho WHERE ocs_id = @ocs AND data_criacao = '2026-10-01 08:00:03');
INSERT INTO PreGuia (status, data_criacao, encaminhamento_url, beneficiario_id, ocs_id)
SELECT 'CONCLUIDA', '2026-09-20 09:00:00', 'seed/faturas/900000103.pdf', @benef, @ocs FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM PreGuia WHERE encaminhamento_url = 'seed/faturas/900000103.pdf');
INSERT INTO Guia (pre_guia_id, numero, arquivo_url, data_insercao, inserida_por, validade_ate, data_envio, enviada_por, situacao_execucao, espelho_id)
SELECT p.id_pre_guia, '900000103', 'seed/faturas/900000103.pdf', '2026-09-21 09:00:00', @guias, '2099-12-31', '2026-09-22 09:00:00', @benef, 'REALIZADA', @esp
FROM PreGuia p WHERE p.encaminhamento_url = 'seed/faturas/900000103.pdf' AND NOT EXISTS (SELECT 1 FROM Guia WHERE numero = '900000103');
INSERT INTO GuiaProcedimento (guia_id, cod_dgp, descricao_procedimento, quantidade_autorizada)
SELECT g.id_guia, '40304361', 'Hemograma completo', 1 FROM Guia g WHERE g.numero = '900000103'
  AND NOT EXISTS (SELECT 1 FROM GuiaProcedimento gp WHERE gp.guia_id = g.id_guia AND gp.cod_dgp = '40304361');
INSERT INTO EspelhoItem (espelho_id, guia_procedimento_id, quantidade_sessoes, valor_sessao, valor_contrato_sessao, origem_contrato, observacao, decisao_ocs, motivo_contestacao_ocs)
SELECT @esp, gp.id_guia_procedimento, 1, 22.00, 18.00, 'TABELA', NULL, NULL, NULL
FROM GuiaProcedimento gp JOIN Guia g ON g.id_guia = gp.guia_id WHERE g.numero = '900000103' AND gp.cod_dgp = '40304361'
  AND NOT EXISTS (SELECT 1 FROM EspelhoItem ei WHERE ei.guia_procedimento_id = gp.id_guia_procedimento);
INSERT INTO PreGuia (status, data_criacao, encaminhamento_url, beneficiario_id, ocs_id)
SELECT 'CONCLUIDA', '2026-09-20 09:00:00', 'seed/faturas/900000104.pdf', @benef, @ocs FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM PreGuia WHERE encaminhamento_url = 'seed/faturas/900000104.pdf');
INSERT INTO Guia (pre_guia_id, numero, arquivo_url, data_insercao, inserida_por, validade_ate, data_envio, enviada_por, situacao_execucao, espelho_id)
SELECT p.id_pre_guia, '900000104', 'seed/faturas/900000104.pdf', '2026-09-21 09:00:00', @guias, '2099-12-31', '2026-09-22 09:00:00', @benef, 'REALIZADA', @esp
FROM PreGuia p WHERE p.encaminhamento_url = 'seed/faturas/900000104.pdf' AND NOT EXISTS (SELECT 1 FROM Guia WHERE numero = '900000104');
INSERT INTO GuiaProcedimento (guia_id, cod_dgp, descricao_procedimento, quantidade_autorizada)
SELECT g.id_guia, '40302040', 'Colesterol total', 1 FROM Guia g WHERE g.numero = '900000104'
  AND NOT EXISTS (SELECT 1 FROM GuiaProcedimento gp WHERE gp.guia_id = g.id_guia AND gp.cod_dgp = '40302040');
INSERT INTO EspelhoItem (espelho_id, guia_procedimento_id, quantidade_sessoes, valor_sessao, valor_contrato_sessao, origem_contrato, observacao, decisao_ocs, motivo_contestacao_ocs)
SELECT @esp, gp.id_guia_procedimento, 1, 15.00, NULL, NULL, NULL, NULL, NULL
FROM GuiaProcedimento gp JOIN Guia g ON g.id_guia = gp.guia_id WHERE g.numero = '900000104' AND gp.cod_dgp = '40302040'
  AND NOT EXISTS (SELECT 1 FROM EspelhoItem ei WHERE ei.guia_procedimento_id = gp.id_guia_procedimento);
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'PROTOCOLADA', @setor, 'Sgt Ana Silva', '2026-10-04 09:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'PROTOCOLADA');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'ENVIADA_LISURA', @setor, 'Sgt Ana Silva', '2026-10-04 10:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'ENVIADA_LISURA');

-- Fatura: Aguardando a OCS, dentro do prazo (903/2026)
INSERT INTO Espelho (ocs_id, status, data_criacao, criado_por, data_envio, enviado_por, situacao_fatura, numero_fatura, protocolada_por)
SELECT @ocs, 'ENVIADO', '2026-10-01 08:00:04', @ocsu, '2026-10-02 09:00:00', @ocsu, 'AGUARDANDO_OCS', '903/2026', @setor FROM DUAL
WHERE @ocs IS NOT NULL AND NOT EXISTS (SELECT 1 FROM Espelho WHERE ocs_id = @ocs AND data_criacao = '2026-10-01 08:00:04');
SET @esp = (SELECT id_espelho FROM Espelho WHERE ocs_id = @ocs AND data_criacao = '2026-10-01 08:00:04');
INSERT INTO PreGuia (status, data_criacao, encaminhamento_url, beneficiario_id, ocs_id)
SELECT 'CONCLUIDA', '2026-09-20 09:00:00', 'seed/faturas/900000105.pdf', @benef, @ocs FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM PreGuia WHERE encaminhamento_url = 'seed/faturas/900000105.pdf');
INSERT INTO Guia (pre_guia_id, numero, arquivo_url, data_insercao, inserida_por, validade_ate, data_envio, enviada_por, situacao_execucao, espelho_id)
SELECT p.id_pre_guia, '900000105', 'seed/faturas/900000105.pdf', '2026-09-21 09:00:00', @guias, '2099-12-31', '2026-09-22 09:00:00', @benef, 'REALIZADA', @esp
FROM PreGuia p WHERE p.encaminhamento_url = 'seed/faturas/900000105.pdf' AND NOT EXISTS (SELECT 1 FROM Guia WHERE numero = '900000105');
INSERT INTO GuiaProcedimento (guia_id, cod_dgp, descricao_procedimento, quantidade_autorizada)
SELECT g.id_guia, '40301630', 'Glicose', 2 FROM Guia g WHERE g.numero = '900000105'
  AND NOT EXISTS (SELECT 1 FROM GuiaProcedimento gp WHERE gp.guia_id = g.id_guia AND gp.cod_dgp = '40301630');
INSERT INTO EspelhoItem (espelho_id, guia_procedimento_id, quantidade_sessoes, valor_sessao, valor_contrato_sessao, origem_contrato, observacao, decisao_ocs, motivo_contestacao_ocs)
SELECT @esp, gp.id_guia_procedimento, 2, 15.00, 12.00, 'MANUAL', 'Valor acima do contrato vigente', NULL, NULL
FROM GuiaProcedimento gp JOIN Guia g ON g.id_guia = gp.guia_id WHERE g.numero = '900000105' AND gp.cod_dgp = '40301630'
  AND NOT EXISTS (SELECT 1 FROM EspelhoItem ei WHERE ei.guia_procedimento_id = gp.id_guia_procedimento);
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'PROTOCOLADA', @setor, 'Sgt Ana Silva', '2026-10-05 09:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'PROTOCOLADA');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'ENVIADA_LISURA', @setor, 'Sgt Ana Silva', '2026-10-05 10:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'ENVIADA_LISURA');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'ENVIADA_OCS', @rast, 'Cb Bruno Lima', NOW() FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'ENVIADA_OCS');
INSERT INTO EspelhoItemRodadaOcs (espelho_item_id, envio_evento_id, valor_contrato_sessao, origem_contrato, decisao_ocs, motivo_contestacao_ocs, decisao_evento_id)
SELECT ei.id_espelho_item, ev.id_fatura_evento, 12.00, 'MANUAL', NULL, NULL,
       NULL
FROM EspelhoItem ei JOIN GuiaProcedimento gp ON gp.id_guia_procedimento = ei.guia_procedimento_id JOIN Guia g ON g.id_guia = gp.guia_id
JOIN FaturaEvento ev ON ev.espelho_id = @esp AND ev.tipo = 'ENVIADA_OCS'
WHERE g.numero = '900000105'
  AND NOT EXISTS (SELECT 1 FROM EspelhoItemRodadaOcs x WHERE x.espelho_item_id = ei.id_espelho_item AND x.envio_evento_id = ev.id_fatura_evento);

-- Fatura: Aguardando a OCS, prazo vencido (904/2026)
INSERT INTO Espelho (ocs_id, status, data_criacao, criado_por, data_envio, enviado_por, situacao_fatura, numero_fatura, protocolada_por)
SELECT @ocs, 'ENVIADO', '2026-10-01 08:00:05', @ocsu, '2026-10-02 09:00:00', @ocsu, 'AGUARDANDO_OCS', '904/2026', @setor FROM DUAL
WHERE @ocs IS NOT NULL AND NOT EXISTS (SELECT 1 FROM Espelho WHERE ocs_id = @ocs AND data_criacao = '2026-10-01 08:00:05');
SET @esp = (SELECT id_espelho FROM Espelho WHERE ocs_id = @ocs AND data_criacao = '2026-10-01 08:00:05');
INSERT INTO PreGuia (status, data_criacao, encaminhamento_url, beneficiario_id, ocs_id)
SELECT 'CONCLUIDA', '2026-09-20 09:00:00', 'seed/faturas/900000106.pdf', @benef, @ocs FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM PreGuia WHERE encaminhamento_url = 'seed/faturas/900000106.pdf');
INSERT INTO Guia (pre_guia_id, numero, arquivo_url, data_insercao, inserida_por, validade_ate, data_envio, enviada_por, situacao_execucao, espelho_id)
SELECT p.id_pre_guia, '900000106', 'seed/faturas/900000106.pdf', '2026-09-21 09:00:00', @guias, '2099-12-31', '2026-09-22 09:00:00', @benef, 'REALIZADA', @esp
FROM PreGuia p WHERE p.encaminhamento_url = 'seed/faturas/900000106.pdf' AND NOT EXISTS (SELECT 1 FROM Guia WHERE numero = '900000106');
INSERT INTO GuiaProcedimento (guia_id, cod_dgp, descricao_procedimento, quantidade_autorizada)
SELECT g.id_guia, '40302040', 'Colesterol total', 1 FROM Guia g WHERE g.numero = '900000106'
  AND NOT EXISTS (SELECT 1 FROM GuiaProcedimento gp WHERE gp.guia_id = g.id_guia AND gp.cod_dgp = '40302040');
INSERT INTO EspelhoItem (espelho_id, guia_procedimento_id, quantidade_sessoes, valor_sessao, valor_contrato_sessao, origem_contrato, observacao, decisao_ocs, motivo_contestacao_ocs)
SELECT @esp, gp.id_guia_procedimento, 1, 20.00, 15.00, 'MANUAL', NULL, NULL, NULL
FROM GuiaProcedimento gp JOIN Guia g ON g.id_guia = gp.guia_id WHERE g.numero = '900000106' AND gp.cod_dgp = '40302040'
  AND NOT EXISTS (SELECT 1 FROM EspelhoItem ei WHERE ei.guia_procedimento_id = gp.id_guia_procedimento);
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'PROTOCOLADA', @setor, 'Sgt Ana Silva', '2026-10-06 09:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'PROTOCOLADA');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'ENVIADA_LISURA', @setor, 'Sgt Ana Silva', '2026-10-06 10:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'ENVIADA_LISURA');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'ENVIADA_OCS', @rast, 'Cb Bruno Lima', '2026-10-07 09:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'ENVIADA_OCS');
INSERT INTO EspelhoItemRodadaOcs (espelho_item_id, envio_evento_id, valor_contrato_sessao, origem_contrato, decisao_ocs, motivo_contestacao_ocs, decisao_evento_id)
SELECT ei.id_espelho_item, ev.id_fatura_evento, 15.00, 'MANUAL', NULL, NULL,
       NULL
FROM EspelhoItem ei JOIN GuiaProcedimento gp ON gp.id_guia_procedimento = ei.guia_procedimento_id JOIN Guia g ON g.id_guia = gp.guia_id
JOIN FaturaEvento ev ON ev.espelho_id = @esp AND ev.tipo = 'ENVIADA_OCS'
WHERE g.numero = '900000106'
  AND NOT EXISTS (SELECT 1 FROM EspelhoItemRodadaOcs x WHERE x.espelho_item_id = ei.id_espelho_item AND x.envio_evento_id = ev.id_fatura_evento);

-- Fatura: Em auditoria, glosa aceita pela OCS (905/2026)
INSERT INTO Espelho (ocs_id, status, data_criacao, criado_por, data_envio, enviado_por, situacao_fatura, numero_fatura, protocolada_por)
SELECT @ocs, 'ENVIADO', '2026-10-01 08:00:06', @ocsu, '2026-10-02 09:00:00', @ocsu, 'EM_AUDITORIA', '905/2026', @setor FROM DUAL
WHERE @ocs IS NOT NULL AND NOT EXISTS (SELECT 1 FROM Espelho WHERE ocs_id = @ocs AND data_criacao = '2026-10-01 08:00:06');
SET @esp = (SELECT id_espelho FROM Espelho WHERE ocs_id = @ocs AND data_criacao = '2026-10-01 08:00:06');
INSERT INTO PreGuia (status, data_criacao, encaminhamento_url, beneficiario_id, ocs_id)
SELECT 'CONCLUIDA', '2026-09-20 09:00:00', 'seed/faturas/900000107.pdf', @benef, @ocs FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM PreGuia WHERE encaminhamento_url = 'seed/faturas/900000107.pdf');
INSERT INTO Guia (pre_guia_id, numero, arquivo_url, data_insercao, inserida_por, validade_ate, data_envio, enviada_por, situacao_execucao, espelho_id)
SELECT p.id_pre_guia, '900000107', 'seed/faturas/900000107.pdf', '2026-09-21 09:00:00', @guias, '2099-12-31', '2026-09-22 09:00:00', @benef, 'REALIZADA', @esp
FROM PreGuia p WHERE p.encaminhamento_url = 'seed/faturas/900000107.pdf' AND NOT EXISTS (SELECT 1 FROM Guia WHERE numero = '900000107');
INSERT INTO GuiaProcedimento (guia_id, cod_dgp, descricao_procedimento, quantidade_autorizada)
SELECT g.id_guia, '40304361', 'Hemograma completo', 2 FROM Guia g WHERE g.numero = '900000107'
  AND NOT EXISTS (SELECT 1 FROM GuiaProcedimento gp WHERE gp.guia_id = g.id_guia AND gp.cod_dgp = '40304361');
INSERT INTO EspelhoItem (espelho_id, guia_procedimento_id, quantidade_sessoes, valor_sessao, valor_contrato_sessao, origem_contrato, observacao, decisao_ocs, motivo_contestacao_ocs)
SELECT @esp, gp.id_guia_procedimento, 2, 25.00, 18.00, 'TABELA', NULL, 'OK', NULL
FROM GuiaProcedimento gp JOIN Guia g ON g.id_guia = gp.guia_id WHERE g.numero = '900000107' AND gp.cod_dgp = '40304361'
  AND NOT EXISTS (SELECT 1 FROM EspelhoItem ei WHERE ei.guia_procedimento_id = gp.id_guia_procedimento);
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'PROTOCOLADA', @setor, 'Sgt Ana Silva', '2026-10-08 09:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'PROTOCOLADA');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'ENVIADA_LISURA', @setor, 'Sgt Ana Silva', '2026-10-08 10:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'ENVIADA_LISURA');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'ENVIADA_OCS', @rast, 'Cb Bruno Lima', '2026-10-09 09:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'ENVIADA_OCS');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'CIENTE_OCS', @ocsu, 'Dr. Carlos Lab', '2026-10-10 09:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'CIENTE_OCS');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'ENVIADA_AUDITORIA', @rast, 'Cb Bruno Lima', '2026-10-10 15:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'ENVIADA_AUDITORIA');
INSERT INTO EspelhoItemRodadaOcs (espelho_item_id, envio_evento_id, valor_contrato_sessao, origem_contrato, decisao_ocs, motivo_contestacao_ocs, decisao_evento_id)
SELECT ei.id_espelho_item, ev.id_fatura_evento, 18.00, 'TABELA', 'OK', NULL,
       (SELECT d.id_fatura_evento FROM FaturaEvento d WHERE d.espelho_id = @esp AND d.tipo = 'CIENTE_OCS')
FROM EspelhoItem ei JOIN GuiaProcedimento gp ON gp.id_guia_procedimento = ei.guia_procedimento_id JOIN Guia g ON g.id_guia = gp.guia_id
JOIN FaturaEvento ev ON ev.espelho_id = @esp AND ev.tipo = 'ENVIADA_OCS'
WHERE g.numero = '900000107'
  AND NOT EXISTS (SELECT 1 FROM EspelhoItemRodadaOcs x WHERE x.espelho_item_id = ei.id_espelho_item AND x.envio_evento_id = ev.id_fatura_evento);

-- Fatura: Auditada (concluída) (906/2026)
INSERT INTO Espelho (ocs_id, status, data_criacao, criado_por, data_envio, enviado_por, situacao_fatura, numero_fatura, protocolada_por)
SELECT @ocs, 'ENVIADO', '2026-10-01 08:00:07', @ocsu, '2026-10-02 09:00:00', @ocsu, 'AUDITADA', '906/2026', @setor FROM DUAL
WHERE @ocs IS NOT NULL AND NOT EXISTS (SELECT 1 FROM Espelho WHERE ocs_id = @ocs AND data_criacao = '2026-10-01 08:00:07');
SET @esp = (SELECT id_espelho FROM Espelho WHERE ocs_id = @ocs AND data_criacao = '2026-10-01 08:00:07');
INSERT INTO PreGuia (status, data_criacao, encaminhamento_url, beneficiario_id, ocs_id)
SELECT 'CONCLUIDA', '2026-09-20 09:00:00', 'seed/faturas/900000108.pdf', @benef, @ocs FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM PreGuia WHERE encaminhamento_url = 'seed/faturas/900000108.pdf');
INSERT INTO Guia (pre_guia_id, numero, arquivo_url, data_insercao, inserida_por, validade_ate, data_envio, enviada_por, situacao_execucao, espelho_id)
SELECT p.id_pre_guia, '900000108', 'seed/faturas/900000108.pdf', '2026-09-21 09:00:00', @guias, '2099-12-31', '2026-09-22 09:00:00', @benef, 'REALIZADA', @esp
FROM PreGuia p WHERE p.encaminhamento_url = 'seed/faturas/900000108.pdf' AND NOT EXISTS (SELECT 1 FROM Guia WHERE numero = '900000108');
INSERT INTO GuiaProcedimento (guia_id, cod_dgp, descricao_procedimento, quantidade_autorizada)
SELECT g.id_guia, '40301630', 'Glicose', 1 FROM Guia g WHERE g.numero = '900000108'
  AND NOT EXISTS (SELECT 1 FROM GuiaProcedimento gp WHERE gp.guia_id = g.id_guia AND gp.cod_dgp = '40301630');
INSERT INTO EspelhoItem (espelho_id, guia_procedimento_id, quantidade_sessoes, valor_sessao, valor_contrato_sessao, origem_contrato, observacao, decisao_ocs, motivo_contestacao_ocs)
SELECT @esp, gp.id_guia_procedimento, 1, 14.00, 12.00, 'MANUAL', NULL, 'OK', NULL
FROM GuiaProcedimento gp JOIN Guia g ON g.id_guia = gp.guia_id WHERE g.numero = '900000108' AND gp.cod_dgp = '40301630'
  AND NOT EXISTS (SELECT 1 FROM EspelhoItem ei WHERE ei.guia_procedimento_id = gp.id_guia_procedimento);
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'PROTOCOLADA', @setor, 'Sgt Ana Silva', '2026-10-11 09:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'PROTOCOLADA');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'ENVIADA_LISURA', @setor, 'Sgt Ana Silva', '2026-10-11 10:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'ENVIADA_LISURA');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'ENVIADA_OCS', @rast, 'Cb Bruno Lima', '2026-10-12 09:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'ENVIADA_OCS');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'CIENTE_OCS', @ocsu, 'Dr. Carlos Lab', '2026-10-13 09:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'CIENTE_OCS');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'ENVIADA_AUDITORIA', @rast, 'Cb Bruno Lima', '2026-10-13 15:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'ENVIADA_AUDITORIA');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'VISUALIZADA', @aud, 'Ten Diana Souza', '2026-10-15 10:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'VISUALIZADA');
INSERT INTO EspelhoItemRodadaOcs (espelho_item_id, envio_evento_id, valor_contrato_sessao, origem_contrato, decisao_ocs, motivo_contestacao_ocs, decisao_evento_id)
SELECT ei.id_espelho_item, ev.id_fatura_evento, 12.00, 'MANUAL', 'OK', NULL,
       (SELECT d.id_fatura_evento FROM FaturaEvento d WHERE d.espelho_id = @esp AND d.tipo = 'CIENTE_OCS')
FROM EspelhoItem ei JOIN GuiaProcedimento gp ON gp.id_guia_procedimento = ei.guia_procedimento_id JOIN Guia g ON g.id_guia = gp.guia_id
JOIN FaturaEvento ev ON ev.espelho_id = @esp AND ev.tipo = 'ENVIADA_OCS'
WHERE g.numero = '900000108'
  AND NOT EXISTS (SELECT 1 FROM EspelhoItemRodadaOcs x WHERE x.espelho_item_id = ei.id_espelho_item AND x.envio_evento_id = ev.id_fatura_evento);

-- Fatura: Em auditoria pelo caminho sem glosa (907/2026)
INSERT INTO Espelho (ocs_id, status, data_criacao, criado_por, data_envio, enviado_por, situacao_fatura, numero_fatura, protocolada_por)
SELECT @ocs, 'ENVIADO', '2026-10-01 08:00:08', @ocsu, '2026-10-02 09:00:00', @ocsu, 'EM_AUDITORIA', '907/2026', @setor FROM DUAL
WHERE @ocs IS NOT NULL AND NOT EXISTS (SELECT 1 FROM Espelho WHERE ocs_id = @ocs AND data_criacao = '2026-10-01 08:00:08');
SET @esp = (SELECT id_espelho FROM Espelho WHERE ocs_id = @ocs AND data_criacao = '2026-10-01 08:00:08');
INSERT INTO PreGuia (status, data_criacao, encaminhamento_url, beneficiario_id, ocs_id)
SELECT 'CONCLUIDA', '2026-09-20 09:00:00', 'seed/faturas/900000109.pdf', @benef, @ocs FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM PreGuia WHERE encaminhamento_url = 'seed/faturas/900000109.pdf');
INSERT INTO Guia (pre_guia_id, numero, arquivo_url, data_insercao, inserida_por, validade_ate, data_envio, enviada_por, situacao_execucao, espelho_id)
SELECT p.id_pre_guia, '900000109', 'seed/faturas/900000109.pdf', '2026-09-21 09:00:00', @guias, '2099-12-31', '2026-09-22 09:00:00', @benef, 'REALIZADA', @esp
FROM PreGuia p WHERE p.encaminhamento_url = 'seed/faturas/900000109.pdf' AND NOT EXISTS (SELECT 1 FROM Guia WHERE numero = '900000109');
INSERT INTO GuiaProcedimento (guia_id, cod_dgp, descricao_procedimento, quantidade_autorizada)
SELECT g.id_guia, '40302040', 'Colesterol total', 1 FROM Guia g WHERE g.numero = '900000109'
  AND NOT EXISTS (SELECT 1 FROM GuiaProcedimento gp WHERE gp.guia_id = g.id_guia AND gp.cod_dgp = '40302040');
INSERT INTO EspelhoItem (espelho_id, guia_procedimento_id, quantidade_sessoes, valor_sessao, valor_contrato_sessao, origem_contrato, observacao, decisao_ocs, motivo_contestacao_ocs)
SELECT @esp, gp.id_guia_procedimento, 1, 15.00, 15.00, 'CONFERIDO', NULL, NULL, NULL
FROM GuiaProcedimento gp JOIN Guia g ON g.id_guia = gp.guia_id WHERE g.numero = '900000109' AND gp.cod_dgp = '40302040'
  AND NOT EXISTS (SELECT 1 FROM EspelhoItem ei WHERE ei.guia_procedimento_id = gp.id_guia_procedimento);
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'PROTOCOLADA', @setor, 'Sgt Ana Silva', '2026-10-14 09:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'PROTOCOLADA');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'ENVIADA_LISURA', @setor, 'Sgt Ana Silva', '2026-10-14 10:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'ENVIADA_LISURA');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'ENVIADA_AUDITORIA', @rast, 'Cb Bruno Lima', '2026-10-15 09:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'ENVIADA_AUDITORIA');

-- Fatura: Em lisura de novo, voltou de contestação da OCS (908/2026)
INSERT INTO Espelho (ocs_id, status, data_criacao, criado_por, data_envio, enviado_por, situacao_fatura, numero_fatura, protocolada_por)
SELECT @ocs, 'ENVIADO', '2026-10-01 08:00:09', @ocsu, '2026-10-02 09:00:00', @ocsu, 'EM_LISURA', '908/2026', @setor FROM DUAL
WHERE @ocs IS NOT NULL AND NOT EXISTS (SELECT 1 FROM Espelho WHERE ocs_id = @ocs AND data_criacao = '2026-10-01 08:00:09');
SET @esp = (SELECT id_espelho FROM Espelho WHERE ocs_id = @ocs AND data_criacao = '2026-10-01 08:00:09');
INSERT INTO PreGuia (status, data_criacao, encaminhamento_url, beneficiario_id, ocs_id)
SELECT 'CONCLUIDA', '2026-09-20 09:00:00', 'seed/faturas/900000110.pdf', @benef, @ocs FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM PreGuia WHERE encaminhamento_url = 'seed/faturas/900000110.pdf');
INSERT INTO Guia (pre_guia_id, numero, arquivo_url, data_insercao, inserida_por, validade_ate, data_envio, enviada_por, situacao_execucao, espelho_id)
SELECT p.id_pre_guia, '900000110', 'seed/faturas/900000110.pdf', '2026-09-21 09:00:00', @guias, '2099-12-31', '2026-09-22 09:00:00', @benef, 'REALIZADA', @esp
FROM PreGuia p WHERE p.encaminhamento_url = 'seed/faturas/900000110.pdf' AND NOT EXISTS (SELECT 1 FROM Guia WHERE numero = '900000110');
INSERT INTO GuiaProcedimento (guia_id, cod_dgp, descricao_procedimento, quantidade_autorizada)
SELECT g.id_guia, '40304361', 'Hemograma completo', 1 FROM Guia g WHERE g.numero = '900000110'
  AND NOT EXISTS (SELECT 1 FROM GuiaProcedimento gp WHERE gp.guia_id = g.id_guia AND gp.cod_dgp = '40304361');
INSERT INTO EspelhoItem (espelho_id, guia_procedimento_id, quantidade_sessoes, valor_sessao, valor_contrato_sessao, origem_contrato, observacao, decisao_ocs, motivo_contestacao_ocs)
SELECT @esp, gp.id_guia_procedimento, 1, 24.00, 18.00, 'TABELA', NULL, 'OK', NULL
FROM GuiaProcedimento gp JOIN Guia g ON g.id_guia = gp.guia_id WHERE g.numero = '900000110' AND gp.cod_dgp = '40304361'
  AND NOT EXISTS (SELECT 1 FROM EspelhoItem ei WHERE ei.guia_procedimento_id = gp.id_guia_procedimento);
INSERT INTO PreGuia (status, data_criacao, encaminhamento_url, beneficiario_id, ocs_id)
SELECT 'CONCLUIDA', '2026-09-20 09:00:00', 'seed/faturas/900000111.pdf', @benef, @ocs FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM PreGuia WHERE encaminhamento_url = 'seed/faturas/900000111.pdf');
INSERT INTO Guia (pre_guia_id, numero, arquivo_url, data_insercao, inserida_por, validade_ate, data_envio, enviada_por, situacao_execucao, espelho_id)
SELECT p.id_pre_guia, '900000111', 'seed/faturas/900000111.pdf', '2026-09-21 09:00:00', @guias, '2099-12-31', '2026-09-22 09:00:00', @benef, 'REALIZADA', @esp
FROM PreGuia p WHERE p.encaminhamento_url = 'seed/faturas/900000111.pdf' AND NOT EXISTS (SELECT 1 FROM Guia WHERE numero = '900000111');
INSERT INTO GuiaProcedimento (guia_id, cod_dgp, descricao_procedimento, quantidade_autorizada)
SELECT g.id_guia, '40301630', 'Glicose', 1 FROM Guia g WHERE g.numero = '900000111'
  AND NOT EXISTS (SELECT 1 FROM GuiaProcedimento gp WHERE gp.guia_id = g.id_guia AND gp.cod_dgp = '40301630');
INSERT INTO EspelhoItem (espelho_id, guia_procedimento_id, quantidade_sessoes, valor_sessao, valor_contrato_sessao, origem_contrato, observacao, decisao_ocs, motivo_contestacao_ocs)
SELECT @esp, gp.id_guia_procedimento, 1, 15.00, NULL, NULL, NULL, 'CONTESTADA', 'O contrato vigente prevê R$ 15,00 por sessão.'
FROM GuiaProcedimento gp JOIN Guia g ON g.id_guia = gp.guia_id WHERE g.numero = '900000111' AND gp.cod_dgp = '40301630'
  AND NOT EXISTS (SELECT 1 FROM EspelhoItem ei WHERE ei.guia_procedimento_id = gp.id_guia_procedimento);
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'PROTOCOLADA', @setor, 'Sgt Ana Silva', '2026-10-16 09:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'PROTOCOLADA');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'ENVIADA_LISURA', @setor, 'Sgt Ana Silva', '2026-10-16 10:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'ENVIADA_LISURA');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'ENVIADA_OCS', @rast, 'Cb Bruno Lima', '2026-10-17 09:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'ENVIADA_OCS');
INSERT INTO FaturaEvento (espelho_id, tipo, usuario_id, responsavel, data_evento)
SELECT @esp, 'CONTESTADA_OCS', @ocsu, 'Dr. Carlos Lab', '2026-10-18 09:00:00' FROM DUAL
WHERE @esp IS NOT NULL AND NOT EXISTS (SELECT 1 FROM FaturaEvento WHERE espelho_id = @esp AND tipo = 'CONTESTADA_OCS');
INSERT INTO EspelhoItemRodadaOcs (espelho_item_id, envio_evento_id, valor_contrato_sessao, origem_contrato, decisao_ocs, motivo_contestacao_ocs, decisao_evento_id)
SELECT ei.id_espelho_item, ev.id_fatura_evento, 18.00, 'TABELA', 'OK', NULL,
       (SELECT d.id_fatura_evento FROM FaturaEvento d WHERE d.espelho_id = @esp AND d.tipo = 'CONTESTADA_OCS')
FROM EspelhoItem ei JOIN GuiaProcedimento gp ON gp.id_guia_procedimento = ei.guia_procedimento_id JOIN Guia g ON g.id_guia = gp.guia_id
JOIN FaturaEvento ev ON ev.espelho_id = @esp AND ev.tipo = 'ENVIADA_OCS'
WHERE g.numero = '900000110'
  AND NOT EXISTS (SELECT 1 FROM EspelhoItemRodadaOcs x WHERE x.espelho_item_id = ei.id_espelho_item AND x.envio_evento_id = ev.id_fatura_evento);
INSERT INTO EspelhoItemRodadaOcs (espelho_item_id, envio_evento_id, valor_contrato_sessao, origem_contrato, decisao_ocs, motivo_contestacao_ocs, decisao_evento_id)
SELECT ei.id_espelho_item, ev.id_fatura_evento, 12.00, 'MANUAL', 'CONTESTADA', 'O contrato vigente prevê R$ 15,00 por sessão.',
       (SELECT d.id_fatura_evento FROM FaturaEvento d WHERE d.espelho_id = @esp AND d.tipo = 'CONTESTADA_OCS')
FROM EspelhoItem ei JOIN GuiaProcedimento gp ON gp.id_guia_procedimento = ei.guia_procedimento_id JOIN Guia g ON g.id_guia = gp.guia_id
JOIN FaturaEvento ev ON ev.espelho_id = @esp AND ev.tipo = 'ENVIADA_OCS'
WHERE g.numero = '900000111'
  AND NOT EXISTS (SELECT 1 FROM EspelhoItemRodadaOcs x WHERE x.espelho_item_id = ei.id_espelho_item AND x.envio_evento_id = ev.id_fatura_evento);

-- ---------------------------------------------------------------------
-- T28-BE2 (SCRUM-249): as Guias de exemplo ficam completas, iguais as que o fluxo real produz.
-- (1) Assinaturas do medico e do chefe em AssinaturaGuia (com linhas de Assinatura de exemplo), para nao aparecerem
--     em "Guias para assinar": a pre-guia ja esta Concluida. Usuario.assinatura_id nao muda (o medico, o chefe e o
--     beneficiario continuam sem assinatura cadastrada, como no seed de contas).
-- (2) Sessoes assinadas pelo beneficiario (uma por unidade da quantidade autorizada), coerentes com as linhas do
--     espelho: as Guias de exemplo estao Realizadas.
-- (3) O PDF de cada Guia (Guia.arquivo_url) e as imagens das assinaturas de exemplo sao gravados no armazenamento
--     local pelo ArquivosExemploSeedRunner, so no perfil dev, ao subir o ambiente.
-- ---------------------------------------------------------------------
SET @medico = (SELECT id_usuario FROM Usuario WHERE login = '11144477735');
SET @chefe = (SELECT id_usuario FROM Usuario WHERE login = 'chefe.fusex');

INSERT INTO Assinatura (usuario_id, assinatura_url)
SELECT @medico, 'seed/assinaturas/medico-exemplo.png' FROM DUAL
WHERE @medico IS NOT NULL AND NOT EXISTS (SELECT 1 FROM Assinatura WHERE usuario_id = @medico AND assinatura_url = 'seed/assinaturas/medico-exemplo.png');
INSERT INTO Assinatura (usuario_id, assinatura_url)
SELECT @chefe, 'seed/assinaturas/chefe-exemplo.png' FROM DUAL
WHERE @chefe IS NOT NULL AND NOT EXISTS (SELECT 1 FROM Assinatura WHERE usuario_id = @chefe AND assinatura_url = 'seed/assinaturas/chefe-exemplo.png');
INSERT INTO Assinatura (usuario_id, assinatura_url)
SELECT @benef, 'seed/assinaturas/beneficiario-exemplo.png' FROM DUAL
WHERE @benef IS NOT NULL AND NOT EXISTS (SELECT 1 FROM Assinatura WHERE usuario_id = @benef AND assinatura_url = 'seed/assinaturas/beneficiario-exemplo.png');

SET @assMedico = (SELECT id_assinatura FROM Assinatura WHERE usuario_id = @medico AND assinatura_url = 'seed/assinaturas/medico-exemplo.png');
SET @assChefe = (SELECT id_assinatura FROM Assinatura WHERE usuario_id = @chefe AND assinatura_url = 'seed/assinaturas/chefe-exemplo.png');
SET @assBenef = (SELECT id_assinatura FROM Assinatura WHERE usuario_id = @benef AND assinatura_url = 'seed/assinaturas/beneficiario-exemplo.png');

INSERT INTO AssinaturaGuia (guia_id, papel, usuario_id, data_assinatura, assinatura_id)
SELECT g.id_guia, 'MEDICO', @medico, g.data_insercao, @assMedico
FROM Guia g
WHERE g.arquivo_url LIKE 'seed/faturas/%' AND @assMedico IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM AssinaturaGuia ag WHERE ag.guia_id = g.id_guia AND ag.papel = 'MEDICO');
INSERT INTO AssinaturaGuia (guia_id, papel, usuario_id, data_assinatura, assinatura_id)
SELECT g.id_guia, 'CHEFE', @chefe, g.data_insercao, @assChefe
FROM Guia g
WHERE g.arquivo_url LIKE 'seed/faturas/%' AND @assChefe IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM AssinaturaGuia ag WHERE ag.guia_id = g.id_guia AND ag.papel = 'CHEFE');

INSERT INTO Sessao (guia_procedimento_id, numero, status, data_solicitacao, solicitada_por, data_assinatura, assinado_por, assinatura_id)
SELECT gp.id_guia_procedimento, n.numero, 'ASSINADA', g.data_envio, @ocsu, g.data_envio, @benef, @assBenef
FROM GuiaProcedimento gp
JOIN Guia g ON g.id_guia = gp.guia_id
JOIN (SELECT 1 AS numero UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5) n
  ON n.numero <= gp.quantidade_autorizada
WHERE g.arquivo_url LIKE 'seed/faturas/%' AND g.situacao_execucao = 'REALIZADA' AND @assBenef IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM Sessao s WHERE s.guia_procedimento_id = gp.id_guia_procedimento AND s.numero = n.numero);
