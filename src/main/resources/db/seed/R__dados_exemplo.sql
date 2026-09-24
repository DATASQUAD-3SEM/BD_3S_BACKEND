-- Dados de EXEMPLO para desenvolvimento e testes (todo mundo fica com os mesmos dados).
-- "R__" = migration repetivel: roda de novo se este arquivo mudar. Por isso so insere se ainda nao existir.
-- NAO e carregado em producao (producao usa so classpath:db/migration).

INSERT INTO procedimento_exame (codigo_tuss, terminologia_procedimento_evento, grupo)
SELECT '40304361', 'Hemograma com contagem de plaquetas ou estimativa', 'Exames laboratoriais' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM procedimento_exame WHERE codigo_tuss = '40304361');

INSERT INTO procedimento_exame (codigo_tuss, terminologia_procedimento_evento, grupo)
SELECT '40302040', 'Glicose', 'Exames laboratoriais' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM procedimento_exame WHERE codigo_tuss = '40302040');

INSERT INTO procedimento_exame (codigo_tuss, terminologia_procedimento_evento, grupo)
SELECT '40301630', 'Colesterol total', 'Exames laboratoriais' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM procedimento_exame WHERE codigo_tuss = '40301630');

INSERT INTO ocs (contrato_num, nome, tipo, inicio_vigencia, termino_vigencia, dias_para_vencimento)
SELECT 'EXEMPLO-001', 'Laboratorio Exemplo', 'LABORATORIO', '2026-01-01', '2027-01-01', 100 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ocs WHERE contrato_num = 'EXEMPLO-001');

INSERT INTO ocs (contrato_num, nome, tipo, inicio_vigencia, termino_vigencia, dias_para_vencimento)
SELECT 'EXEMPLO-002', 'Hospital Exemplo', 'HOSPITAL', '2026-01-01', '2027-06-01', 250 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ocs WHERE contrato_num = 'EXEMPLO-002');

-- EXEMPLO-001 realiza os 3 exames; EXEMPLO-002 realiza so o hemograma
INSERT INTO ocs_procedimento (ocs_id, procedimento_id)
SELECT o.id, p.id FROM ocs o, procedimento_exame p
WHERE o.contrato_num = 'EXEMPLO-001'
  AND p.codigo_tuss IN ('40304361', '40302040', '40301630')
  AND NOT EXISTS (SELECT 1 FROM ocs_procedimento x WHERE x.ocs_id = o.id AND x.procedimento_id = p.id);

INSERT INTO ocs_procedimento (ocs_id, procedimento_id)
SELECT o.id, p.id FROM ocs o, procedimento_exame p
WHERE o.contrato_num = 'EXEMPLO-002'
  AND p.codigo_tuss = '40304361'
  AND NOT EXISTS (SELECT 1 FROM ocs_procedimento x WHERE x.ocs_id = o.id AND x.procedimento_id = p.id);
