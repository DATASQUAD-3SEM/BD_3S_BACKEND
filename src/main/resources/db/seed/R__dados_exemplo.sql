-- =====================================================================
-- Procedimentos (ProcedimentoExame)
-- rol_ans: ajuste para o valor real do rol quando tiver a informacao
-- =====================================================================
INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '40304361', 'Hemograma com contagem de plaquetas ou estimativa', 'NAO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '40304361');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '40302040', 'Glicose', 'NAO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '40302040');

INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '40301630', 'Colesterol total', 'NAO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '40301630');


-- =====================================================================
-- OCS (Ocs)
-- =====================================================================
INSERT INTO Ocs (contrato_num, nome, tipo)
SELECT 'EXEMPLO-001', 'Laboratorio Exemplo', 'OCS' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = 'EXEMPLO-001');

INSERT INTO Ocs (contrato_num, nome, tipo)
SELECT 'EXEMPLO-002', 'Hospital Exemplo', 'PSA' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = 'EXEMPLO-002');

-- EXEMPLO-001 realiza os 3 exames; EXEMPLO-002 realiza so o hemograma
INSERT INTO OcsProcedimento (ocs_id, procedimento_id)
SELECT o.id_ocs, p.id_procedimento_exame
FROM Ocs o, ProcedimentoExame p
WHERE o.contrato_num = 'EXEMPLO-001'
  AND p.codigo_tuss IN ('40304361', '40302040', '40301630')
  AND NOT EXISTS (
    SELECT 1 FROM OcsProcedimento x
    WHERE x.ocs_id = o.id_ocs AND x.procedimento_id = p.id_procedimento_exame);

INSERT INTO OcsProcedimento (ocs_id, procedimento_id)
SELECT o.id_ocs, p.id_procedimento_exame
FROM Ocs o, ProcedimentoExame p
WHERE o.contrato_num = 'EXEMPLO-002'
  AND p.codigo_tuss = '40304361'
  AND NOT EXISTS (
    SELECT 1 FROM OcsProcedimento x
    WHERE x.ocs_id = o.id_ocs AND x.procedimento_id = p.id_procedimento_exame);


-- =====================================================================
-- Contas (Usuario) e perfis
-- Logins de CPF usam 11 digitos. Chefe e setores usam nome de usuario.
-- =====================================================================

-- Beneficiario de teste
INSERT INTO Usuario (login, senha_hash, perfil, assinatura_url)
SELECT '52998224725', 'HASH_BCRYPT_AQUI', 'BENEFICIARIO', NULL FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = '52998224725');

INSERT INTO Beneficiario (usuario_id, nome, prec_cp, data_nascimento, telefone)
SELECT u.id_usuario, 'Antonio Carlos Ferreira', '45872213300', '1968-03-15', '12987654321'
FROM Usuario u
WHERE u.login = '52998224725'
  AND NOT EXISTS (SELECT 1 FROM Beneficiario b WHERE b.usuario_id = u.id_usuario);

-- Medico de teste
INSERT INTO Usuario (login, senha_hash, perfil, assinatura_url)
SELECT '11144477735', 'HASH_BCRYPT_AQUI', 'MEDICO', NULL FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = '11144477735');

INSERT INTO MedicoFusex (usuario_id, nome, prec_cp, patente, crm)
SELECT u.id_usuario, 'Carlos Eduardo Lima', '11122233344', 'Capitao', '123456-SP'
FROM Usuario u
WHERE u.login = '11144477735'
  AND NOT EXISTS (SELECT 1 FROM MedicoFusex m WHERE m.usuario_id = u.id_usuario);

-- Chefe do FUSEX (conta de seed: nao ha tela para criar o primeiro chefe)
INSERT INTO Usuario (login, senha_hash, perfil, assinatura_url)
SELECT 'chefe.fusex', 'HASH_BCRYPT_AQUI', 'CHEFE_FUSEX', NULL FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = 'chefe.fusex');

INSERT INTO ChefeFusex (usuario_id, nome, patente)
SELECT u.id_usuario, 'Maria Souza', 'Coronel'
FROM Usuario u
WHERE u.login = 'chefe.fusex'
  AND NOT EXISTS (SELECT 1 FROM ChefeFusex c WHERE c.usuario_id = u.id_usuario);

-- Setor de Guias (conta de setor: sem pessoa atribuida, sem assinatura)
INSERT INTO Usuario (login, senha_hash, perfil, assinatura_url)
SELECT 'setor.guias', 'HASH_BCRYPT_AQUI', 'SETOR_GUIAS', NULL FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = 'setor.guias');

-- Recepcao (conta de setor: sem pessoa atribuida, sem assinatura)
INSERT INTO Usuario (login, senha_hash, perfil, assinatura_url)
SELECT 'recepcao', 'HASH_BCRYPT_AQUI', 'RECEPCAO', NULL FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = 'recepcao');