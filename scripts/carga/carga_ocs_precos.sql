-- Carga T28-BE2 (SCRUM-249): OCS/PSA reais + ProcedimentoExame (TUSS) + OcsPreco. Script de uso unico, fora de
-- db/migration: rodar depois das migrations (a tabela OcsPreco e a coluna descricao vem da T25-BE7, V17/V18).
-- Fontes: "TABELA DE PARAMETROS DE PRECOS - UG-FUSEX.xls" (cliente) e "cruzamento_tuss_precos_ocs.xlsx".
-- Idempotente: a OCS e identificada por contrato_num + nome (o nº de contrato e por OCS e se repete: "20/24"
-- aparece em tres prestadores), o catalogo por codigo_tuss e o preco por ocs_id + cod_dgp.
-- Precos: so os 214 da aba AUTO (valor fixo por sessao e correspondencia TUSS de alta confianca). Os 434 da aba
-- REVISAR ficam para o time confirmar na planilha; os 934 da aba MANUAL ficam para a lisura manual (T25-BE2).

-- 1) As 55 OCS/PSA da tabela de parametros (com ou sem preco AUTO)
INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '15/21', 'CARDIOVITA SERVIÇOS MÉDICOS LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '15/21' AND nome = 'CARDIOVITA SERVIÇOS MÉDICOS LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '01/2023', 'CENTROCOR - CENTRO DE DIAGNÓSTICO E CIRURGIA DO CORAÇÃO LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '01/2023' AND nome = 'CENTROCOR - CENTRO DE DIAGNÓSTICO E CIRURGIA DO CORAÇÃO LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '17/2022', 'CENTRO ONCOLÓGICO DO VALE', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '17/2022' AND nome = 'CENTRO ONCOLÓGICO DO VALE');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '04/2023', 'CLÍNICA ORTOPÉDICA SANTA LUCIA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '04/2023' AND nome = 'CLÍNICA ORTOPÉDICA SANTA LUCIA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '26/2021', 'ORTOPEDIA E FISIOTERAPIA MONTELLO LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '26/2021' AND nome = 'ORTOPEDIA E FISIOTERAPIA MONTELLO LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '35/2021', 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '35/2021' AND nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '04/2022', 'CLÍNICA DE UROGINECOLOGIA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '04/2022' AND nome = 'CLÍNICA DE UROGINECOLOGIA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '32/2021', 'IPMMI-HOSPITAL MATERNO INFANTIL ANTONINHO DA ROCHA MARMO', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '32/2021' AND nome = 'IPMMI-HOSPITAL MATERNO INFANTIL ANTONINHO DA ROCHA MARMO');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '10/2021', 'FISIO VIP CLIN LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '10/2021' AND nome = 'FISIO VIP CLIN LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '14/2021', 'LABORATÓRIO DE ANÁLISES CLÍNICAS SÃO JOSE', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '14/2021' AND nome = 'LABORATÓRIO DE ANÁLISES CLÍNICAS SÃO JOSE');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '28/2021', 'M.A. MONTANHA RUGGERI LABORATÓRIO - EPP', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '28/2021' AND nome = 'M.A. MONTANHA RUGGERI LABORATÓRIO - EPP');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '29/2021', 'CLARO E MARTINS CLÍNICA ODONTOLÓGICA LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '29/2021' AND nome = 'CLARO E MARTINS CLÍNICA ODONTOLÓGICA LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '13/2022', 'L&L PSICOLOGIA LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '13/2022' AND nome = 'L&L PSICOLOGIA LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '12/2022', 'HOME CARE SILVA E SILVA ASSISTÊNCIA A SAÚDE', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '12/2022' AND nome = 'HOME CARE SILVA E SILVA ASSISTÊNCIA A SAÚDE');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '11/2021', 'SÃO JOSE ASSISTÊNCIA A SAÚDE EIRELI - HOME CARE', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '11/2021' AND nome = 'SÃO JOSE ASSISTÊNCIA A SAÚDE EIRELI - HOME CARE');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '09/2024', 'ONCOVIDA ONCO-HEMATOLOGIA LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '09/2024' AND nome = 'ONCOVIDA ONCO-HEMATOLOGIA LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '16/2020', 'ASSOCIAÇÃO DE EQUOTERAPIA SAÚDE E EDUCAÇÃO TROTANDO EM FRENTE LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '16/2020' AND nome = 'ASSOCIAÇÃO DE EQUOTERAPIA SAÚDE E EDUCAÇÃO TROTANDO EM FRENTE LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '22/2020', 'SERVASC CLÍNICA VASCULAR LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '22/2020' AND nome = 'SERVASC CLÍNICA VASCULAR LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '11/2020', 'ENDOCARD CLÍNICA MÉDICA D.B.F. LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '11/2020' AND nome = 'ENDOCARD CLÍNICA MÉDICA D.B.F. LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '15/2020', 'COE CENTRO DE OLHOS E ESPECIALIDADES', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '15/2020' AND nome = 'COE CENTRO DE OLHOS E ESPECIALIDADES');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '13/2020', 'PERSONALCLIN - PRESTAÇÃO DE SERVIÇOS DE SAÚDE EIRELI', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '13/2020' AND nome = 'PERSONALCLIN - PRESTAÇÃO DE SERVIÇOS DE SAÚDE EIRELI');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '18/2020', 'CLÍNICA DE EMAGRECIMENTO E BEM ESTAR', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '18/2020' AND nome = 'CLÍNICA DE EMAGRECIMENTO E BEM ESTAR');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '10/2020', 'ODONTO CLEAN LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '10/2020' AND nome = 'ODONTO CLEAN LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '28/2020', 'CEMEV-CLÍNICA DE ESPECIALIDADES MÉDICAS DO VALE LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '28/2020' AND nome = 'CEMEV-CLÍNICA DE ESPECIALIDADES MÉDICAS DO VALE LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '13/2021', 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '13/2021' AND nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '06/2021', 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '06/2021' AND nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '09/2020', 'QUAGGIO SERVIÇOS ODONTOLÓGICOS LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '09/2020' AND nome = 'QUAGGIO SERVIÇOS ODONTOLÓGICOS LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '24/2021', 'MASTERCLIN CLÍNICA FISIOTERAPÊUTICA LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '24/2021' AND nome = 'MASTERCLIN CLÍNICA FISIOTERAPÊUTICA LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '30/2021', 'PLANI DIAGNÓSTICOS MÉDICOS LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '30/2021' AND nome = 'PLANI DIAGNÓSTICOS MÉDICOS LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '34/2021', 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '34/2021' AND nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '20/2021', 'CENTRO DE VALORIZAÇÃO DA VIDA FRANCISCA JULIA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '20/2021' AND nome = 'CENTRO DE VALORIZAÇÃO DA VIDA FRANCISCA JULIA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '14/2023', 'INSTITUTO DE RADIOTERAPIA DO VALE DO PARAÍBA LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '14/2023' AND nome = 'INSTITUTO DE RADIOTERAPIA DO VALE DO PARAÍBA LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '06/2022', 'Dr. LUIZ GUSTAVO CENTURION DE MOURA', 'PSA', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '06/2022' AND nome = 'Dr. LUIZ GUSTAVO CENTURION DE MOURA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '14/2022', 'Dra. NAZARÉ JAQUELINE DE LIMA MARQUES', 'PSA', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '14/2022' AND nome = 'Dra. NAZARÉ JAQUELINE DE LIMA MARQUES');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '10/2024', 'VID LAP SS LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '10/2024' AND nome = 'VID LAP SS LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '12/2024', 'CENTRO DIAGNÓSTICO ANDRADE', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '12/2024' AND nome = 'CENTRO DIAGNÓSTICO ANDRADE');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '08/2024', 'HEMACORE - SERVIÇO DE HEMATOLOGIA DE SJC LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '08/2024' AND nome = 'HEMACORE - SERVIÇO DE HEMATOLOGIA DE SJC LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '17/24', 'SERVIÇOS DE TRATAMENTO AO CÂNCER DE TAUBATÉ LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '17/24' AND nome = 'SERVIÇOS DE TRATAMENTO AO CÂNCER DE TAUBATÉ LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '15/24', 'HOSPITAL DE OLHOS DO VALE - EXCIMER LASER VALE DO PARAIBA LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '15/24' AND nome = 'HOSPITAL DE OLHOS DO VALE - EXCIMER LASER VALE DO PARAIBA LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '20/24', 'DRA. HERIKA THAMILLA CAMARGO ALVES ALBANO FARIA', 'PSA', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '20/24' AND nome = 'DRA. HERIKA THAMILLA CAMARGO ALVES ALBANO FARIA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '19/2024', 'IPMMI-HOSPITALPIO XII', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '19/2024' AND nome = 'IPMMI-HOSPITALPIO XII');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '02/25', 'NXT TLECONSULTA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '02/25' AND nome = 'NXT TLECONSULTA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '20/24', 'CLÍNICA DE NEURODESENVOLVIMENTO TEAR MAIS', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '20/24' AND nome = 'CLÍNICA DE NEURODESENVOLVIMENTO TEAR MAIS');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '46/25', 'CLÍNICA PEQUENO GESTO', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '46/25' AND nome = 'CLÍNICA PEQUENO GESTO');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '50/2025', 'CLÍNICA ROTA DA APRENDIZAGEM', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '50/2025' AND nome = 'CLÍNICA ROTA DA APRENDIZAGEM');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '61/2025', 'CLÍNICA AUMERA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '61/2025' AND nome = 'CLÍNICA AUMERA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '66/2025', 'WK RADIOLOGIA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '66/2025' AND nome = 'WK RADIOLOGIA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '64/2025', 'ESPAÇO NEURO&PSI LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '64/2025' AND nome = 'ESPAÇO NEURO&PSI LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '58/2025', 'CLINÍCA MÉDICA ESSENZA LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '58/2025' AND nome = 'CLINÍCA MÉDICA ESSENZA LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '72/2025', 'PRO IMAGEM PINDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '72/2025' AND nome = 'PRO IMAGEM PINDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '73/2025', 'PRO IMAGEM TAUBATÉ', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '73/2025' AND nome = 'PRO IMAGEM TAUBATÉ');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '44/2025', 'INTERDIÁLOGOS TRANSFORMAÇÃO PLURAL LTDA', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '44/2025' AND nome = 'INTERDIÁLOGOS TRANSFORMAÇÃO PLURAL LTDA');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '45/2025', 'KARINA PRATES', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '45/2025' AND nome = 'KARINA PRATES');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '48/2025', '3B BASLER', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '48/2025' AND nome = '3B BASLER');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT '20/24', 'SINAPSE', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = '20/24' AND nome = 'SINAPSE');

-- 2) Codigos TUSS novos usados abaixo, para o catalogo ProcedimentoExame
INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '10101039', 'Consulta em pronto socorro', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '10101039');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20101104', 'Avaliação da composição corporal por bioimpedanciometria', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20101104');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103034', 'Amputação bilateral (treinamento protético)', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103034');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103050', 'Amputação unilateral (treinamento protético)', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103050');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103220', 'Doenças pulmonares atendidas em ambulatório', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103220');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103239', 'Exercícios de ortóptica (por sessão)', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103239');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103263', 'Hemiparesia', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103263');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103271', 'Hemiplegia', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103271');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103280', 'Hemiplegia e hemiparesia com afasia', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103280');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103310', 'Lesão nervosa periférica afetando mais de um nervo com alterações sensitivas e/ou motoras', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103310');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103328', 'Lesão nervosa periférica afetando um nervo com alterações sensitivas e/ou motoras', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103328');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103344', 'Miopatias', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103344');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103433', 'Paralisia cerebral com distúrbio de comunicação', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103433');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103441', 'Paraparesia/tetraparesia', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103441');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103468', 'Parkinson', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103468');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103476', 'Patologia neurológica com dependência de atividades da vida diária', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103476');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103484', 'Patologia osteomioarticular em um membro', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103484');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103492', 'Patologia osteomioarticular em dois ou mais membros', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103492');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103506', 'Patologia osteomioarticular em um segmento da coluna', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103506');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103514', 'Patologia osteomioarticular em diferentes segmentos da coluna', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103514');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103522', 'Patologias osteomioarticulares com dependência de atividades da vida diária', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103522');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103620', 'Reabilitação de paciente com endoprótese', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103620');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103654', 'Recuperação funcional de distúrbios crânio-faciais', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103654');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103689', 'Retardo do desenvolvimento psicomotor', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103689');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20103700', 'Sequelas em politraumatizados (em diferentes segmentos)', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20103700');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20104294', 'Terapia oncológica - planejamento e 1º dia de tratamento', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20104294');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20104308', 'Terapia oncológica - por dia subsequente de tratamento', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20104308');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '20104421', 'Terapia imunobiológica subcutânea (por sessão) ambulatorial', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '20104421');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30304083', 'Implante de anel intra-estromal', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30304083');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30304105', 'Delaminação corneana com fotoablação estromal - LASIK', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30304105');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30310040', 'Cirurgias fistulizantes com implantes valvulares', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30310040');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30312086', 'Retinopexia com introflexão escleral', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30312086');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30313015', 'Cirurgia da glândula lacrimal', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30313015');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30313058', 'Reconstituição de vias lacrimais com silicone ou outro material', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30313058');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30709016', 'Instalação de halo craniano', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30709016');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30709024', 'Tração cutânea', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30709024');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30709032', 'Tração transesquelética (por membro)', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30709032');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30710057', 'Retirada de fixadores externos', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30710057');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30712017', 'Áxilo-palmar ou pendente', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30712017');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30712025', 'Bota com ou sem salto', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30712025');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30712033', 'Colar', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30712033');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30712041', 'Colete', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30712041');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30712050', 'Cruro-podálico', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30712050');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30712068', 'Dupla abdução ou Ducroquet', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30712068');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30712076', 'Halo-gesso', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30712076');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30712084', 'Inguino-maleolar', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30712084');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30712092', 'Luva', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30712092');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30712106', 'Minerva ou Risser para escoliose', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30712106');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30712114', 'Pelvipodálico', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30712114');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30712122', 'Spica-gessada', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30712122');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30712130', 'Tipo Velpeau', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30712130');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30712149', 'Tóraco-braquial', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30712149');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30713048', 'Enxertos em outras pseudartroses', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30713048');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30713072', 'Retirada de enxerto ósseo', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30713072');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30715040', 'Biópsia de corpo vertebral com agulha', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30715040');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30715130', 'Fratura do cóccix - redução incruenta', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30715130');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30719054', 'Biópsia cirúrgica de cotovelo', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30719054');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30720044', 'Biópsia cirúrgica do antebraço', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30720044');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30721032', 'Artrodese entre os ossos do carpo', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30721032');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30721059', 'Artrodese rádio-cárpica ou do punho', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30721059');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30721121', 'Encurtamento rádio/ulnar', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30721121');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30721156', 'Fratura do carpo - redução incruenta', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30721156');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30721202', 'Luxação do carpo - redução incruenta', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30721202');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30721237', 'Reparação ligamentar do carpo', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30721237');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30721253', 'Transposição do rádio para ulna', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30721253');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30722160', 'Biópsia cirúrgica dos ossos da mão', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30722160');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30722209', 'Capsulectomias múltiplas MF ou IF', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30722209');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30722217', 'Capsulectomias única MF e IF', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30722217');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30722322', 'Falangização', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30722322');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30722373', 'Fratura de osso da mão - tratamento conservador', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30722373');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30722560', 'Policização ou transferência digital', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30722560');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30722640', 'Reconstrução de leito ungueal', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30722640');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30722683', 'Reimplante do polegar', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30722683');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30722713', 'Ressecção de cisto sinovial', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30722713');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30722780', 'Sequestrectomias', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30722780');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30722870', 'Tratamento da doença de Kiembuck com transplante vascularizado', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30722870');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30722888', 'Tratamento da pseudoartrose do escafóide com transplante ósseo vascularizado e fixação com micro parafuso', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30722888');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30724112', 'Biópsia cirúrgica coxo-femoral', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30724112');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30725046', 'Biópsia cirúrgica de fêmur', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30725046');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30725186', 'Fêmur - descolamento epifisário de extremidades superior - tratamento conservador sem gesso', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30725186');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30726140', 'Lesões agudas e/ou luxações de meniscos (1 ou ambos) - tratamento cirúrgico', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30726140');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30727057', 'Biópsia cirúrgica de tíbia ou fíbula', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30727057');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30727197', 'Fratura de dois ossos da perna - tratamento conservador', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30727197');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30728088', 'Biópsia cirúrgica do tornozelo', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30728088');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30729068', 'Biópsia cirúrgica dos ossos do pé', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30729068');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30729173', 'Fraturas e/ou luxações do antepé - tratamento cirúrgico', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30729173');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30729254', 'Retração cicatricial dos dedos', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30729254');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30730015', 'Alongamento', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30730015');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30730058', 'Dissecção muscular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30730058');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30730066', 'Drenagem cirúrgica do psoas', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30730066');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30730074', 'Fasciotomia', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30730074');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30730090', 'Fasciotomias (descompressivas)', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30730090');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30730104', 'Fasciotomias acima do punho', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30730104');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30730112', 'Miorrafias', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30730112');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30730155', 'Transposição muscular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30730155');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30731089', 'Tenodese', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30731089');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30731097', 'Tenólise no túnel osteofibroso', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30731097');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30731135', 'Tenorrafia múltipla em outras regiões', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30731135');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30731160', 'Tenorrafia única em outras regiões', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30731160');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30731178', 'Tenossinovectomia de mão ou punho', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30731178');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30731194', 'Tenossinovites infecciosas - drenagem', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30731194');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30731208', 'Tenotomia', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30731208');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30731224', 'Transposição única de tendão', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30731224');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30732093', 'Tumor ósseo (ressecção e artrodese)', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30732093');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30732107', 'Tumor ósseo (ressecção e cimento)', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30732107');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30732115', 'Tumor ósseo (ressecção e enxerto)', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30732115');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30732123', 'Tumor ósseo (ressecção segmentar)', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30732123');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30732131', 'Tumor ósseo (ressecção simples)', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30732131');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30733022', 'Sinovectomia parcial ou subtotal - procedimento videoartroscópico de joelho', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30733022');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30733081', 'Fratura com redução e/ou estabilização da superfície articular - um compartimento # - procedimento videoartroscópico de joelho', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30733081');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30734029', 'Sinovectomia parcial ou subtotal - procedimento videoartroscópico de tornozelo', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30734029');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30734045', 'Osteocondroplastia - estabilização, ressecção e ou plastia (enxertia) # - procedimento videoartroscópico de tornozelo', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30734045');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30736013', 'Sinovectomia total - procedimento videoartroscópico de cotovelo', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30736013');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30736021', 'Sinovectomia parcial ou subtotal - procedimento videoartroscópico de cotovelo', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30736021');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30736030', 'Condroplastia (com remoção de corpos livres) - procedimento videoartroscópico de cotovelo', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30736030');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30736048', 'Osteocondroplastia - estabilização, ressecção e/ou plastia (enxertia) # - procedimento videoartroscópico de cotovelo', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30736048');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30736056', 'Reconstrução, retencionamento ou reforço de ligamento # - procedimento videoartroscópico de cotovelo', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30736056');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30736064', 'Fraturas: redução e estabilização para cada superfície - procedimento videoartroscópico de cotovelo', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30736064');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30737079', 'Túnel do carpo - descompressão - procedimento videoartroscópico de punho e túnel do carpo', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30737079');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30738016', 'Sinovectomia total - procedimento videoartroscópico de coxofemoral', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30738016');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30738024', 'Sinovectomia parcial e/ou remoção de corpos livres - procedimento videoartroscópico de coxofemoral', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30738024');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '30738059', 'Condroplastia com sutura labral - procedimento videoartroscópico de coxofemoral', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '30738059');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '31403255', 'Microneurorrafia de dedos da mão', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '31403255');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '31403280', 'Neurólise das síndromes compressivas', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '31403280');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '40103242', 'Eletro-oculografia - monocular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '40103242');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '40103250', 'Eletro-retinografia - monocular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '40103250');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '40103633', 'Potencial evocado visual (PEV)', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '40103633');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '40201120', 'Endoscopia digestiva alta', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '40201120');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '40308561', 'Anti transglutaminase tecidual - IgG', 'NAO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '40308561');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '40324176', 'Chikungunya, anticorpos', 'NAO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '40324176');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '40901106', 'Ecodopplercardiograma transtorácico', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '40901106');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '40901521', 'Ultrassonografia biomicroscópica - monocular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '40901521');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '40901530', 'Ultrassonografia diagnóstica - monocular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '40901530');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '41301013', 'Angiofluoresceinografia - monocular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '41301013');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '41301021', 'Angiografia com indocianina verde - monocular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '41301021');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '41301030', 'Avaliação órbito-palpebral-exoftalmometria - binocular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '41301030');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '41301129', 'Curva tensional diária - binocular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '41301129');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '41301153', 'Estéreo-foto de papila - monocular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '41301153');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '41301200', 'Exame de motilidade ocular (teste ortóptico) - binocular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '41301200');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '41301242', 'Gonioscopia - binocular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '41301242');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '41301277', 'Oftalmodinamometria - monocular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '41301277');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '41301315', 'Retinografia (só honorário) monocular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '41301315');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '41301366', 'Visão subnormal - monocular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '41301366');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '41301439', 'Fundoscopia sob medríases - binocular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '41301439');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '41401271', 'Teste de sensibilidade de contraste ou de cores - monocular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '41401271');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '41401301', 'Teste provocativo para glaucoma - binocular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '41401301');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '41501144', 'Tomografia de coerência óptica - monocular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '41501144');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '50000080', 'Sessão individual ambulatorial, em terapia ocupacional', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '50000080');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '50000144', 'Consulta ambulatorial em fisioterapia', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '50000144');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '50000152', 'Sessão para assistência fisioterapêutica ambulatorial ao paciente com disfunção decorrente de lesão do sistema nervoso central e/ou periférico', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '50000152');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '50000160', 'Sessão para assistência fisioterapêutica ambulatorial ao paciente com disfunção decorrente de alterações do sistema músculo-esquelético', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '50000160');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '50000179', 'Sessão para assistência fisioterapêutica ambulatorial ao paciente com disfunção decorrente de alterações no sistema respiratório', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '50000179');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '50000187', 'Sessão para assistência fisioterapêutica ambulatorial ao paciente com disfunção decorrente de alterações do sistema cardiovascular', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '50000187');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '50000195', 'Sessão para assistência fisioterapêutica ambulatorial ao paciente com disfunção decorrente de queimaduras', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '50000195');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '50000209', 'Sessão para assistência fisioterapêutica ambulatorial ao paciente com disfunção decorrente de alterações do sistema linfático e/ou vascular periférico', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '50000209');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '50000217', 'Sessão para assistência fisioterapêutica ambulatorial no pré e pós cirúrgico e em recuperação de tecidos', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '50000217');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '50000225', 'Sessão para assistência fisioterapêutica ambulatorial por alterações endocrino-metabólicas', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '50000225');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '50000233', 'Sessão para assistência fisioterapêutica ambulatorial para alterações inflamatórias e ou degenerativas do aparelho genito-urinário e reprodutor', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '50000233');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '50000438', 'Hidroterapia', 'NAO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '50000438');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '50000446', 'RPG', 'NAO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '50000446');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '50000462', 'Consulta em psicologia', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '50000462');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '50000470', 'Sessão de psicoterapia individual por psicólogo', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '50000470');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '50000616', 'Sessão individual ambulatorial de fonoaudiologia', 'SIM' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '50000616');

-- 3) Precos de contrato (OcsPreco) da aba AUTO; descricao = texto do procedimento na tabela do cliente
INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30304083', 'Implante de anel intra-estronal', 4100.00
FROM Ocs o WHERE o.contrato_num = '48/2025' AND o.nome = '3B BASLER'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30304083');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30304083', 'Implante de Anel Intraestromal', 3345.60
FROM Ocs o WHERE o.contrato_num = '28/2020' AND o.nome = 'CEMEV-CLÍNICA DE ESPECIALIDADES MÉDICAS DO VALE LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30304083');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30304105', 'Delaminação corneana com fotoablação estromal - LASIK', 2198.96
FROM Ocs o WHERE o.contrato_num = '28/2020' AND o.nome = 'CEMEV-CLÍNICA DE ESPECIALIDADES MÉDICAS DO VALE LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30304105');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30310040', 'Cirurgias Fistulizantes com Implantes Valvulares', 4150.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30310040');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30312086', 'Retinopexia com introflexão Escleral', 1900.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30312086');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30709016', 'instalacao de halo craniano', 200.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30709016');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30709024', 'tracao cutanea', 80.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30709024');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30709032', 'tracao transesqueletica (por mem', 180.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30709032');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30710057', 'retirada de fixadores externos', 264.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30710057');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30712017', 'axilopalmar ou pendente', 60.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30712017');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30712025', 'bota com ou sem salto', 60.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30712025');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30712033', 'colar', 40.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30712033');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30712041', 'colete', 80.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30712041');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30712050', 'cruro-podalico', 80.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30712050');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30712068', 'dupla abducao ou ducroquet', 80.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30712068');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30712076', 'halo gesso', 250.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30712076');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30712084', 'inguino maleolar', 60.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30712084');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30712092', 'luva', 40.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30712092');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30712106', 'minerva ou risser para escoliose', 200.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30712106');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30712114', 'pelvipodalico', 200.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30712114');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30712122', 'spica gessada', 150.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30712122');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30712130', 'tipo velpeau', 100.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30712130');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30712149', 'toracobraquial', 150.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30712149');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30713048', 'enxertos em outras pseudartroses', 800.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30713048');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30713072', 'retirada de enxerto osseo', 400.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30713072');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30715040', 'biopsia de corpo vertebral com a', 200.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30715040');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30715130', 'fratura do coccix - reducao incr', 250.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30715130');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30719054', 'biopsia cirurgica do cotovelo', 300.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30719054');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30720044', 'biopsia cirurgica do antebraco -', 250.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30720044');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30721032', 'artrodese entre os ossos do carp', 550.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30721032');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30721059', 'artrodese radio-carpica ou do pu', 550.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30721059');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30721121', 'encurtamento radio/ulna', 600.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30721121');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30721156', 'fratura do carpo - reducao incru', 300.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30721156');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30721202', 'luxacao do carpo - reducao incru', 200.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30721202');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30721237', 'reparacao ligamentar do carpo', 700.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30721237');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30721253', 'transposicao do radio para ulna', 850.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30721253');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30722160', 'biopsia cirurgica dos ossos da m', 250.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30722160');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30722209', 'capsulectomias multiplas mf ou i', 300.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30722209');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30722217', 'capsulectomia unica mf e if', 250.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30722217');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30722322', 'falangizacao', 500.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30722322');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30722373', 'fratura de osso da mao - tratamento conservador', 87.28
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30722373');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30722560', 'policizacao ou trasferencia digi', 1200.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30722560');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30722640', 'reconstrucao de leito ungueal', 387.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30722640');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30722683', 'reimplante do polegar', 2000.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30722683');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30722713', 'resseccao de cisto sinovial', 250.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30722713');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30722780', 'sequestrectomias', 300.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30722780');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30722870', 'tratamento da doenca de kiembuck com transplante vascularizado', 470.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30722870');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30722888', 'tratamento da pseudoartrose do escafoide com transplante osseo vascularizado e fixacao com micro parafuso', 1530.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30722888');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30724112', 'biopsia cirurgica do coxo femora', 400.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30724112');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30725046', 'biopsia cirurgica do femur', 400.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30725046');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30725186', 'fêmur - descolamento epifisário de extremidades superior - tratamento conservador sem gesso', 250.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30725186');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30726140', 'lesoes agudas e/ou luxacoes de miniscos (1 ou ambors) - tratamento cirurgico', 1000.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30726140');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30727057', 'biopsia cirurgica de tibia ou fi', 300.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30727057');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30727197', 'fratura de dois ossos da perna - tratamento conservador', 250.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30727197');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30728088', 'biopsia cirurgica do tornozelo', 300.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30728088');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30729068', 'biopsia cirurgica dos ossos do p', 250.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30729068');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30729173', 'fraturas e/ou luxacoes do antepe - tratamento cirurgico', 436.36
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30729173');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30729254', 'retracao cicatricial dos dedos s', 300.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30729254');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30730015', 'alongamento', 350.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30730015');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30730058', 'disseccao muscular', 350.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30730058');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30730066', 'drenagem cirurgica do psoas', 350.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30730066');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30730074', 'fasciotomia', 333.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30730074');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30730090', 'fasciotomias (descompressivas)', 500.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30730090');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30730104', 'fasciotomias acima do punho', 500.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30730104');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30730112', 'miorrafia', 300.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30730112');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30730155', 'transposicao muscular', 400.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30730155');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30731089', 'tenodese', 300.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30731089');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30731097', 'tenolise no tunel osteo fibroso', 700.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30731097');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30731135', 'tenorrafia multipla em outras re', 550.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30731135');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30731160', 'tenorrafia unica em outras regio', 300.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30731160');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30731178', 'tenosinovectomia de mao ou punho', 300.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30731178');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30731194', 'tenosinovites infecciosas - dren', 250.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30731194');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30731208', 'tenotomia', 250.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30731208');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30731224', 'transposicao unica de tendao', 400.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30731224');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30732093', 'tumor osseo (resseccao e artrode', 1083.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30732093');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30732107', 'tumor osseo (resseccao e cimento', 1083.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30732107');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30732115', 'tumor osseo (resseccao e enxerto', 1300.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30732115');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30732123', 'tumor osseo (resseccao segmentar', 333.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30732123');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30732131', 'tumor osseo (resseccao yesples)', 400.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30732131');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30733022', 'sinovectomia parcial ou subtotal - procedimento videoartroscopico em joelho', 1155.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30733022');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30733081', 'fratura com redução e/ou estabilização da superfície articular- um compartimento - procedimento videoartroscópico em joelho', 1890.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30733081');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30734029', 'sinovectomia parcial ou subtotal - procedimento videoartrocopico de tornozelo', 1112.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30734029');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30734045', 'osteocondroplastia - estabilizacao, resseccao e ou plastia (enxertia) -procedimento videoartroscopico de tornozelo', 1050.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30734045');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30736013', 'sinovectomia total - procedimento videoartroscopico de cotovelo', 1155.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30736013');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30736021', 'sinovectomia parcial ou subtotal - procedimento videoartroscopico de cotovelo', 1112.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30736021');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30736030', 'condroplastia (com remocao de corpos livres) -procedimento  video artroscopico de cotovelo', 1112.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30736030');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30736048', 'osteocondroplastia - estabilizacao, resseccao e/ou plastia (enxertia) -procedimento videoartrocopico de cotovelo', 1050.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30736048');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30736056', 'reconstrucao, retencionamento ou reforco de ligamento- procedimento videoartroscopico de cotovelo', 1340.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30736056');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30736064', 'fraturas: reduc ao e estabilizacao para cada superficie - procedimento videoartroscopico de cotovelo', 1890.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30736064');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30737079', 'Tunel do carpo - descompressão-procedimento vedeoartroscópico de punho e tunel do carpo', 1112.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30737079');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30738016', 'sinovectomia total - procedimento videoartroscopico de coxofemoral', 1521.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30738016');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30738024', 'sinovectomia parcial e/ou remoção de corpos livres - procedimento videoartrocopico de coxofemoral', 1179.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30738024');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30738059', 'condroplastia com sutura labral - procedimento videoartroscopico de coxofemoral', 2138.15
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30738059');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '31403255', 'micro-neurorrafia de dedos da ma', 900.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '31403255');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '31403280', 'neurolise das sindromes compress', 550.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '31403280');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40103242', 'ELETRO-OCULOGRAFIA - MONOCULAR', 60.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40103242');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40103250', 'ELETRO-RETINOGRAFIA - MONOCULAR', 65.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40103250');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40103633', 'POTENCIAL EVOCADO VISUAL (PEV)', 95.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40103633');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40901521', 'ULTRA-SONOGRAFIA BIOMICROSCÓPICA - MONOCULAR', 72.25
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40901521');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40901530', 'ULTRA-SONOGRAFIA DIAGNÓSTICA - MONOCULAR', 67.50
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40901530');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301013', 'ANGIOFLUORESCEINOGRAFIA - MONOCULAR', 90.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301013');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301021', 'ANGIOGRAFIA COM INDOCIANINA VERDE - MONOCULAR', 110.50
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301021');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301030', 'AVALIAÇÃO ÓRBITO-PALPEBRAL-EXOFTALMOMETRIA - BINOCULAR', 65.75
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301030');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301129', 'CURVA TENSIONAL DIÁRIA - BINOCULAR', 68.50
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301129');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301153', 'ESTÉREO-FOTO DE PAPILA - MONOCULAR', 51.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301153');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301200', 'EXAME DE MOTILIDADE OCULAR (TESTE ORTÓPTICO) - BINOCULAR', 60.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301200');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301242', 'GONIOSCOPIA - BINOCULAR', 60.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301242');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301277', 'OFTALMODINAMOMETRIA - MONOCULAR', 50.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301277');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301315', 'RETINOGRAFIA (SÓ HONORÁRIO) MONOCULAR', 65.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301315');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301366', 'VISÃO SUBNORMAL - MONOCULAR', 57.50
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301366');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301439', 'FUNDOSCOPIA SOB MEDRÍASES - BINOCULAR', 56.50
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301439');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41401271', 'TESTE DE SENSIBILIDADE DE CONTRASTE OU DE CORES - MONOCULAR', 57.50
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41401271');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41401301', 'TESTE PROVOCATIVO PARA GLAUCOMA - BINOCULAR', 61.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41401301');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41501144', 'TOMOGRAFIA DE COERÊNCIA ÓPTICA - MONOCULAR', 210.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41501144');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '50000152', 'sessao para assistencia fisioterapeutica ambulatorial ao paciente com disfuncao decorrente de lesao do sistema nervoso central e ou periferico', 32.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '50000152');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '50000160', 'sessao para assistencia fisioterapeutica ambulatorial ao paciente com disfuncao decorrente de alteracoes do sistema musculo esqueletico', 32.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '50000160');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '50000179', 'sessao para assistencia fisioterapeutica ambulatorial ao paciente com disfuncao decorrente de alteracoes no sistema respiratorio', 32.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '50000179');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '50000187', 'sessao para assistencia fisioterapeutica ambulatorial ao paciente com disfuncao decorrente de alteracoes do sistema cardiovascular', 32.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '50000187');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '50000195', 'sessao para assistencia fisioterapeutica ambulatorial ao paciente com disfuncao decorrente de queimaduras', 32.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '50000195');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '50000209', 'sessao para assistencia fisioterapeutica ambulatorial ao paciente com disfuncao decorrente de alteracoes do sistema linfatico e ou vascular periferico', 32.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '50000209');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '50000217', 'sessao para assistencia fisioterapeutica ambulatorial no pre e pos cirurgico e em recuperacao de tecidos', 32.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '50000217');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '50000225', 'sessao para assistencia fisioterapeutica ambulatorial por alteracoes endocrino metabolicas', 32.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '50000225');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '50000233', 'sessao para assistencia fisioterapeutica ambulatorial para alteracoes inflamatorias e ou degenerativas do aparelho genito urinario e reprodutor', 32.00
FROM Ocs o WHERE o.contrato_num = '06/2021' AND o.nome = 'CENTRO MÉDICO E OFTALMOLÓGICO ALMEIDA CREPALDI LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '50000233');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40901106', 'Ecodopplercardiograma transtorácico', 175.00
FROM Ocs o WHERE o.contrato_num = '01/2023' AND o.nome = 'CENTROCOR - CENTRO DE DIAGNÓSTICO E CIRURGIA DO CORAÇÃO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40901106');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20101104', 'Avaliação da composição corporal por bioimpedância', 20.00
FROM Ocs o WHERE o.contrato_num = '18/2020' AND o.nome = 'CLÍNICA DE EMAGRECIMENTO E BEM ESTAR'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20101104');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '50000080', 'Sessão individual ambulatorial em Terapia Ocupacional', 50.00
FROM Ocs o WHERE o.contrato_num = '20/24' AND o.nome = 'CLÍNICA DE NEURODESENVOLVIMENTO TEAR MAIS'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '50000080');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '50000470', 'Sessão de psicoterapia Individual por psicológo', 50.00
FROM Ocs o WHERE o.contrato_num = '20/24' AND o.nome = 'CLÍNICA DE NEURODESENVOLVIMENTO TEAR MAIS'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '50000470');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '50000616', 'Sessão individual ambulatorial de fonoaudiologia', 50.00
FROM Ocs o WHERE o.contrato_num = '20/24' AND o.nome = 'CLÍNICA DE NEURODESENVOLVIMENTO TEAR MAIS'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '50000616');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30304083', 'Implante de Anel Intraestromal', 3345.60
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30304083');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30304105', 'Delaminação corneana com fotoablação estromal - LASIK', 2198.96
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30304105');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30310040', 'Cirurgias Fistulizantes com Implantes Valvulares', 4150.00
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30310040');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30312086', 'Retinopexia com introflexão Escleral', 1900.00
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30312086');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30313015', 'Cirúrgica da glândula lacrimal', 2800.00
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30313015');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30313058', 'Reconstituição de vias lacrimais com silicone ou outro material.', 2800.00
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30313058');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40103242', 'ELETRO-OCULOGRAFIA - MONOCULAR', 60.00
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40103242');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40103250', 'ELETRO-RETINOGRAFIA - MONOCULAR', 65.00
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40103250');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40103633', 'POTENCIAL EVOCADO VISUAL (PEV)', 95.00
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40103633');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40901521', 'ULTRA-SONOGRAFIA BIOMICROSCÓPICA - MONOCULAR', 72.25
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40901521');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40901530', 'ULTRA-SONOGRAFIA DIAGNÓSTICA - MONOCULAR', 67.50
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40901530');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301013', 'ANGIOFLUORESCEINOGRAFIA - MONOCULAR', 90.00
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301013');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301021', 'ANGIOGRAFIA COM INDOCIANINA VERDE - MONOCULAR', 110.50
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301021');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301030', 'AVALIAÇÃO ÓRBITO-PALPEBRAL-EXOFTALMOMETRIA - BINOCULAR', 65.75
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301030');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301129', 'CURVA TENSIONAL DIÁRIA - BINOCULAR', 68.50
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301129');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301153', 'ESTÉREO-FOTO DE PAPILA - MONOCULAR', 51.00
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301153');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301200', 'EXAME DE MOTILIDADE OCULAR (TESTE ORTÓPTICO) - BINOCULAR', 60.00
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301200');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301242', 'GONIOSCOPIA - BINOCULAR', 60.00
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301242');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301277', 'OFTALMODINAMOMETRIA - MONOCULAR', 50.00
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301277');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301315', 'RETINOGRAFIA (SÓ HONORÁRIO) MONOCULAR', 65.00
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301315');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301366', 'VISÃO SUBNORMAL - MONOCULAR', 57.50
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301366');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301439', 'FUNDOSCOPIA SOB MEDRÍASES - BINOCULAR', 56.50
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301439');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41401271', 'TESTE DE SENSIBILIDADE DE CONTRASTE OU DE CORES - MONOCULAR', 57.50
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41401271');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41401301', 'TESTE PROVOCATIVO PARA GLAUCOMA - BINOCULAR', 61.00
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41401301');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41501144', 'TOMOGRAFIA DE COERÊNCIA ÓPTICA - MONOCULAR', 210.00
FROM Ocs o WHERE o.contrato_num = '34/2021' AND o.nome = 'CLÍNICA OFTALMOLÓGICA ALMEIDA E VENÂNCIO LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41501144');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103263', 'Hemiparesias', 45.00
FROM Ocs o WHERE o.contrato_num = '10/2021' AND o.nome = 'FISIO VIP CLIN LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103263');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103271', 'Hemiplegias', 45.00
FROM Ocs o WHERE o.contrato_num = '10/2021' AND o.nome = 'FISIO VIP CLIN LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103271');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103484', 'Patologia osteomioarticular de um membro', 35.00
FROM Ocs o WHERE o.contrato_num = '10/2021' AND o.nome = 'FISIO VIP CLIN LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103484');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103492', 'Patologia osteomioarticular em dois ou mais membros', 45.00
FROM Ocs o WHERE o.contrato_num = '10/2021' AND o.nome = 'FISIO VIP CLIN LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103492');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103506', 'Patologia osteomioarticular em um segmento da Coluna', 40.00
FROM Ocs o WHERE o.contrato_num = '10/2021' AND o.nome = 'FISIO VIP CLIN LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103506');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '50000438', 'Hidroterapia', 43.28
FROM Ocs o WHERE o.contrato_num = '10/2021' AND o.nome = 'FISIO VIP CLIN LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '50000438');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '50000446', 'RPG', 50.00
FROM Ocs o WHERE o.contrato_num = '10/2021' AND o.nome = 'FISIO VIP CLIN LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '50000446');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20104294', 'Terapia Oncológica - Planejamento e 1º dia de tratamento', 191.04
FROM Ocs o WHERE o.contrato_num = '08/2024' AND o.nome = 'HEMACORE - SERVIÇO DE HEMATOLOGIA DE SJC LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20104294');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20104308', 'Terapia Oncológica - por dia subsequente de tratamento', 38.58
FROM Ocs o WHERE o.contrato_num = '08/2024' AND o.nome = 'HEMACORE - SERVIÇO DE HEMATOLOGIA DE SJC LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20104308');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '10101039', 'CONSULTA EM PRONTO SOCORRO', 80.08
FROM Ocs o WHERE o.contrato_num = '32/2021' AND o.nome = 'IPMMI-HOSPITAL MATERNO INFANTIL ANTONINHO DA ROCHA MARMO'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '10101039');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103239', 'Exercícios de ortóptica (por sessão)', 55.00
FROM Ocs o WHERE o.contrato_num = '19/2024' AND o.nome = 'IPMMI-HOSPITALPIO XII'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103239');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40201120', 'Endoscopia Digestiva Alta', 580.00
FROM Ocs o WHERE o.contrato_num = '19/2024' AND o.nome = 'IPMMI-HOSPITALPIO XII'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40201120');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40308561', 'Transglutaminase Tecidual – IgG', 28.60
FROM Ocs o WHERE o.contrato_num = '19/2024' AND o.nome = 'IPMMI-HOSPITALPIO XII'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40308561');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40324176', 'Chikungunya, anticorpos', 265.02
FROM Ocs o WHERE o.contrato_num = '19/2024' AND o.nome = 'IPMMI-HOSPITALPIO XII'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40324176');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '50000462', 'Consulta Psicologia', 92.65
FROM Ocs o WHERE o.contrato_num = '09/2024' AND o.nome = 'ONCOVIDA ONCO-HEMATOLOGIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '50000462');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30304083', 'Implante de Anel Intraestromal', 3345.60
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30304083');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30304105', 'Delaminação corneana com fotoablação estromal - LASIK', 2198.96
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30304105');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '30310040', 'CIRURGIAS FISTULIZANTES COM IMPLANTES VALVULARES', 4150.00
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '30310040');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40103242', 'ELETRO-OCULOGRAFIA - MONOCULAR', 60.00
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40103242');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40103250', 'ELETRO-RETINOGRAFIA - MONOCULAR', 65.00
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40103250');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40103633', 'POTENCIAL EVOCADO VISUAL (PEV)', 95.00
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40103633');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40901521', 'ULTRA-SONOGRAFIA BIOMICROSCÓPICA - MONOCULAR', 72.25
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40901521');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '40901530', 'ULTRA-SONOGRAFIA DIAGNÓSTICA - MONOCULAR', 67.50
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '40901530');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301013', 'ANGIOFLUORESCEINOGRAFIA - MONOCULAR', 90.00
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301013');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301021', 'ANGIOGRAFIA COM INDOCIANINA VERDE - MONOCULAR', 110.50
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301021');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301030', 'AVALIAÇÃO ÓRBITO-PALPEBRAL-EXOFTALMOMETRIA - BINOCULAR', 67.75
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301030');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301129', 'CURVA TENSIONAL DIÁRIA - BINOCULAR', 68.50
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301129');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301153', 'ESTÉREO-FOTO DE PAPILA - MONOCULAR', 51.00
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301153');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301200', 'EXAME DE MOTILIDADE OCULAR (TESTE ORTÓPTICO) - BINOCULAR', 60.00
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301200');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301242', 'GONIOSCOPIA - BINOCULAR', 60.00
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301242');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301277', 'OFTALMODINAMOMETRIA - MONOCULAR', 50.00
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301277');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301315', 'RETINOGRAFIA (SÓ HONORÁRIO) MONOCULAR', 65.00
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301315');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301366', 'VISÃO SUBNORMAL - MONOCULAR', 57.50
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301366');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41301439', 'FUNDOSCOPIA SOB MEDRÍASES - BINOCULAR', 56.50
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41301439');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41401271', 'TESTE DE SENSIBILIDADE DE CONTRASTE OU DE CORES - MONOCULAR', 57.50
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41401271');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41401301', 'TESTE PROVOCATIVO PARA GLAUCOMA - BINOCULAR', 61.00
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41401301');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '41501144', 'TOMOGRAFIA DE COERÊNCIA ÓPTICA - MONOCULAR', 210.00
FROM Ocs o WHERE o.contrato_num = '35/2021' AND o.nome = 'RAAJ8 SERVIÇOS EM OFTALMOLOGIA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '41501144');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103034', 'Amputação bilateral (treinamento protético)', 18.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103034');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103050', 'Amputação unilateral (treinamento protético)', 15.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103050');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103220', 'Doenças pulmonares atendidas em ambulatório', 10.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103220');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103280', 'Hemiplegia e hemiparesia com afasia', 15.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103280');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103310', 'Lesão nervosa periférica afetando mais de um nervo com alterações sensitivas e/ou motoras', 12.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103310');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103328', 'Lesão nervosa periférica afetando um nervo com alterações sensitivas ou motoras', 10.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103328');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103344', 'Miopatias', 15.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103344');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103433', 'Paralisia Cerebral com distúrbio de comunicação', 30.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103433');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103441', 'Paraparesia/Tetraparesia', 25.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103441');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103468', 'Parkinson', 30.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103468');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103476', 'Patologia neurológica com dependência de atividades da vida diária', 40.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103476');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103484', 'Patologia osteomioarticular em um membro', 12.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103484');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103492', 'Patologia osteomioarticular em dois ou mais membros', 15.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103492');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103506', 'Patologia osteomioarticular em um segmento da coluna', 12.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103506');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103514', 'Patologia osteomioarticular em diferentes segmentos da coluna', 15.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103514');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103522', 'Patologias osteomioarticular com dependência de atividades de vida diária', 30.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103522');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103620', 'Reabilitação de paciente com endoprótese', 25.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103620');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103654', 'Recuperação funcional de distúrbios crânios-faciais', 15.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103654');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103689', 'Retardo no desenvolvimento psicomotor', 30.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103689');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20103700', 'Sequelas em politraumatizados (em diferentes segmentos)', 25.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20103700');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '50000144', 'Consulta ambulatorial em Fisioterapia', 30.00
FROM Ocs o WHERE o.contrato_num = '13/2021' AND o.nome = 'REABILITAR SAÚDE CLÍNICA DE FISIOTERAPIA LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '50000144');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20104294', '20104394 - Terapia Oncológica – Planejamento e 1º dia de tratamento', 191.04
FROM Ocs o WHERE o.contrato_num = '17/24' AND o.nome = 'SERVIÇOS DE TRATAMENTO AO CÂNCER DE TAUBATÉ LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20104294');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20104308', '20104308 - Terapia Oncológica – Por dia subsequente de tratamento', 38.58
FROM Ocs o WHERE o.contrato_num = '17/24' AND o.nome = 'SERVIÇOS DE TRATAMENTO AO CÂNCER DE TAUBATÉ LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20104308');

INSERT INTO OcsPreco (ocs_id, cod_dgp, descricao, valor_sessao)
SELECT o.id_ocs, '20104421', '20104391 - Terapia imunobiológica subcutânea (por sessão) ambulatorial', 236.26
FROM Ocs o WHERE o.contrato_num = '17/24' AND o.nome = 'SERVIÇOS DE TRATAMENTO AO CÂNCER DE TAUBATÉ LTDA'
  AND NOT EXISTS (SELECT 1 FROM OcsPreco x WHERE x.ocs_id = o.id_ocs AND x.cod_dgp = '20104421');
