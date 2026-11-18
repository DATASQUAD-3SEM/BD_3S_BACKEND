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

-- Procedimento com acento e fora de qualquer OCS (T2-BE3: busca sem acento e no catalogo inteiro)
INSERT INTO ProcedimentoExame (codigo_tuss, terminologia_procedimento, rol_ans)
SELECT '40301150', 'Ácido úrico', 'NAO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM ProcedimentoExame WHERE codigo_tuss = '40301150');


-- =====================================================================
-- OCS (Ocs)
-- =====================================================================
INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT 'EXEMPLO-001', 'Laboratorio Exemplo', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = 'EXEMPLO-001');

INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT 'EXEMPLO-002', 'Hospital Exemplo', 'PSA', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = 'EXEMPLO-002');

-- OCS INATIVA (Para testar o filtro da T2-BE2 do revisor)
INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT 'EXEMPLO-003', 'Clinica Inativa Exemplo', 'OCS', FALSE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = 'EXEMPLO-003');

-- OCS com acento (Para testar a busca accent-insensitive)
INSERT INTO Ocs (contrato_num, nome, tipo, ativo)
SELECT 'EXEMPLO-004', 'Ortopédia e Clínica Médica', 'OCS', TRUE FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Ocs WHERE contrato_num = 'EXEMPLO-004');


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
-- Senha de todas as contas de exemplo: senha123
-- =====================================================================

-- Beneficiario de teste
INSERT INTO Usuario (login, senha_hash, perfil)
SELECT '52998224725', '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO', 'BENEFICIARIO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = '52998224725');

INSERT INTO Beneficiario (usuario_id, nome, prec_cp, data_nascimento, telefone)
SELECT u.id_usuario, 'Antonio Carlos Ferreira', '45872213300', '1968-03-15', '12987654321'
FROM Usuario u
WHERE u.login = '52998224725'
  AND NOT EXISTS (SELECT 1 FROM Beneficiario b WHERE b.usuario_id = u.id_usuario);

-- Medico de teste
INSERT INTO Usuario (login, senha_hash, perfil)
SELECT '11144477735', '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO', 'MEDICO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = '11144477735');

INSERT INTO MedicoFusex (usuario_id, nome, prec_cp, patente, crm)
SELECT u.id_usuario, 'Carlos Eduardo Lima', '11122233344', 'Capitao', '123456-SP'
FROM Usuario u
WHERE u.login = '11144477735'
  AND NOT EXISTS (SELECT 1 FROM MedicoFusex m WHERE m.usuario_id = u.id_usuario);

-- Chefe do FUSEX (conta de seed: nao ha tela para criar o primeiro chefe)
INSERT INTO Usuario (login, senha_hash, perfil)
SELECT 'chefe.fusex', '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO', 'CHEFE_FUSEX' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = 'chefe.fusex');

INSERT INTO ChefeFusex (usuario_id, nome, patente)
SELECT u.id_usuario, 'Maria Souza', 'Coronel'
FROM Usuario u
WHERE u.login = 'chefe.fusex'
  AND NOT EXISTS (SELECT 1 FROM ChefeFusex c WHERE c.usuario_id = u.id_usuario);

-- Setor de Guias (conta de setor: sem pessoa atribuida, sem assinatura)
INSERT INTO Usuario (login, senha_hash, perfil)
SELECT 'setor.guias', '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO', 'SETOR_GUIAS' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = 'setor.guias');

-- Recepcao (conta de setor: sem pessoa atribuida, sem assinatura)
INSERT INTO Usuario (login, senha_hash, perfil)
SELECT 'recepcao', '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO', 'RECEPCAO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = 'recepcao');

-- OCS de teste (conta de OCS: sem pessoa atribuida, sem assinatura), ligada a EXEMPLO-001 por ContaOcs
INSERT INTO Usuario (login, senha_hash, perfil)
SELECT 'ocs.laboratorio', '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO', 'OCS' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = 'ocs.laboratorio');

INSERT INTO ContaOcs (usuario_id, ocs_id)
SELECT u.id_usuario, o.id_ocs
FROM Usuario u
JOIN Ocs o ON o.contrato_num = 'EXEMPLO-001'
WHERE u.login = 'ocs.laboratorio'
  AND NOT EXISTS (SELECT 1 FROM ContaOcs c WHERE c.usuario_id = u.id_usuario)
  AND NOT EXISTS (SELECT 1 FROM ContaOcs c WHERE c.ocs_id = o.id_ocs);

-- Contas criadas por versoes antigas deste seed ficaram com o placeholder.
-- Este UPDATE corrige so essas linhas; senhas ja trocadas pelos usuarios nao sao tocadas.
UPDATE Usuario SET senha_hash = '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO'
WHERE senha_hash = 'HASH_BCRYPT_AQUI';

-- =====================================================================
-- Dependentes e responsaveis legais de exemplo (T29-BE2, para QA das telas de T30 e T31).
-- Titulares: Antonio (52998224725) e Marta (70000000663). Logins de CPF; senha de todas: senha123.
-- Cenarios:
--   Lucas ........ menor, titular Antonio, responsavel so o titular (vigencia determinada)
--   Sofia ........ menor, titular Marta, dois responsaveis: a titular e a externa Renata
--   Helena ....... maior, vitalicia, com conta propria; tambem responsavel legal de Lucas (acumulo de papeis)
--   Pedro ........ maior, vigencia determinada (data de termino futura)
--   Joao ......... ENCERRADO (removido pelo chefe), com historico de responsavel legal de Lucas
-- Idempotente: cada linha e inserida so se a chave natural ainda nao existir.
-- =====================================================================

-- Titular Marta (segundo titular de exemplo)
INSERT INTO Usuario (login, senha_hash, perfil)
SELECT '70000000663', '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO', 'BENEFICIARIO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = '70000000663');

INSERT INTO Beneficiario (usuario_id, nome, prec_cp, data_nascimento, telefone)
SELECT u.id_usuario, 'Marta Souza Lima', '45810020001', '1975-09-02', '11988887777'
FROM Usuario u
WHERE u.login = '70000000663'
  AND NOT EXISTS (SELECT 1 FROM Beneficiario b WHERE b.usuario_id = u.id_usuario);

-- Helena: maior com conta propria (perfil BENEFICIARIO), dependente vitalicia de Antonio
INSERT INTO Usuario (login, senha_hash, perfil)
SELECT '30000000205', '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO', 'BENEFICIARIO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = '30000000205');

INSERT INTO Beneficiario (usuario_id, nome, prec_cp, data_nascimento, telefone)
SELECT u.id_usuario, 'Helena Ferreira', '45872213303', '1970-05-20', '12977776666'
FROM Usuario u
WHERE u.login = '30000000205'
  AND NOT EXISTS (SELECT 1 FROM Beneficiario b WHERE b.usuario_id = u.id_usuario);

-- Joao: maior com conta propria, dependente encerrado de Antonio
INSERT INTO Usuario (login, senha_hash, perfil)
SELECT '50000000485', '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO', 'BENEFICIARIO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = '50000000485');

INSERT INTO Beneficiario (usuario_id, nome, prec_cp, data_nascimento, telefone)
SELECT u.id_usuario, 'Joao Ferreira', '45872213305', '2001-01-01', '12966665555'
FROM Usuario u
WHERE u.login = '50000000485'
  AND NOT EXISTS (SELECT 1 FROM Beneficiario b WHERE b.usuario_id = u.id_usuario);

-- Renata: pessoa externa, sem Prec CP e sem Beneficiario; login pelo CPF; assinatura cadastrada no primeiro acesso
INSERT INTO Usuario (login, senha_hash, perfil)
SELECT '60000000574', '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO', 'RESPONSAVEL_EXTERNO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = '60000000574');

-- Dependentes de Antonio (titular 52998224725)
INSERT INTO Dependente (titular_id, usuario_id, nome, data_nascimento, cpf, telefone, prec_cp, vinculo,
                        tipo_vigencia, vigencia_fim, situacao, data_encerramento)
SELECT b.usuario_id, NULL, 'Lucas Ferreira', '2015-06-10', '98765432100', NULL, '45872213301', 'Filho',
       'DETERMINADA', '2037-06-09', 'ATIVO', NULL
FROM Beneficiario b JOIN Usuario u ON u.id_usuario = b.usuario_id
WHERE u.login = '52998224725'
  AND NOT EXISTS (SELECT 1 FROM Dependente d WHERE d.prec_cp = '45872213301');

INSERT INTO Dependente (titular_id, usuario_id, nome, data_nascimento, cpf, telefone, prec_cp, vinculo,
                        tipo_vigencia, vigencia_fim, situacao, data_encerramento)
SELECT b.usuario_id, (SELECT c.id_usuario FROM Usuario c WHERE c.login = '30000000205'),
       'Helena Ferreira', '1970-05-20', '30000000205', NULL, '45872213303', 'Conjuge',
       'VITALICIA', NULL, 'ATIVO', NULL
FROM Beneficiario b JOIN Usuario u ON u.id_usuario = b.usuario_id
WHERE u.login = '52998224725'
  AND NOT EXISTS (SELECT 1 FROM Dependente d WHERE d.prec_cp = '45872213303');

INSERT INTO Dependente (titular_id, usuario_id, nome, data_nascimento, cpf, telefone, prec_cp, vinculo,
                        tipo_vigencia, vigencia_fim, situacao, data_encerramento)
SELECT b.usuario_id, NULL, 'Pedro Ferreira', '1998-08-08', '40000000396', NULL, '45872213304', 'Irmao',
       'DETERMINADA', '2030-12-31', 'ATIVO', NULL
FROM Beneficiario b JOIN Usuario u ON u.id_usuario = b.usuario_id
WHERE u.login = '52998224725'
  AND NOT EXISTS (SELECT 1 FROM Dependente d WHERE d.prec_cp = '45872213304');

INSERT INTO Dependente (titular_id, usuario_id, nome, data_nascimento, cpf, telefone, prec_cp, vinculo,
                        tipo_vigencia, vigencia_fim, situacao, data_encerramento)
SELECT b.usuario_id, (SELECT c.id_usuario FROM Usuario c WHERE c.login = '50000000485'),
       'Joao Ferreira', '2001-01-01', '50000000485', NULL, '45872213305', 'Filho',
       'VITALICIA', NULL, 'ENCERRADO', '2021-01-05 10:00:00'
FROM Beneficiario b JOIN Usuario u ON u.id_usuario = b.usuario_id
WHERE u.login = '52998224725'
  AND NOT EXISTS (SELECT 1 FROM Dependente d WHERE d.prec_cp = '45872213305');

-- Dependente de Marta (titular 70000000663)
INSERT INTO Dependente (titular_id, usuario_id, nome, data_nascimento, cpf, telefone, prec_cp, vinculo,
                        tipo_vigencia, vigencia_fim, situacao, data_encerramento)
SELECT b.usuario_id, NULL, 'Sofia Souza', '2017-02-01', '20000000108', NULL, '45810020002', 'Filha',
       'DETERMINADA', '2038-01-31', 'ATIVO', NULL
FROM Beneficiario b JOIN Usuario u ON u.id_usuario = b.usuario_id
WHERE u.login = '70000000663'
  AND NOT EXISTS (SELECT 1 FROM Dependente d WHERE d.prec_cp = '45810020002');

-- Responsaveis legais (ResponsavelLegal aponta para Usuario; a linha segue mesmo se o dependente encerrar)
-- Lucas: Antonio desde o nascimento; Helena desde 2019 (acumulo de papeis); Joao de 2019 a 2021 (historico)
INSERT INTO ResponsavelLegal (dependente_id, usuario_id, data_inicio, data_fim)
SELECT d.id_dependente, u.id_usuario, '2015-06-10', NULL
FROM Dependente d JOIN Usuario u ON u.login = '52998224725'
WHERE d.prec_cp = '45872213301'
  AND NOT EXISTS (SELECT 1 FROM ResponsavelLegal r
                  WHERE r.dependente_id = d.id_dependente AND r.usuario_id = u.id_usuario AND r.data_inicio = '2015-06-10');

INSERT INTO ResponsavelLegal (dependente_id, usuario_id, data_inicio, data_fim)
SELECT d.id_dependente, u.id_usuario, '2019-01-01', NULL
FROM Dependente d JOIN Usuario u ON u.login = '30000000205'
WHERE d.prec_cp = '45872213301'
  AND NOT EXISTS (SELECT 1 FROM ResponsavelLegal r
                  WHERE r.dependente_id = d.id_dependente AND r.usuario_id = u.id_usuario AND r.data_inicio = '2019-01-01');

INSERT INTO ResponsavelLegal (dependente_id, usuario_id, data_inicio, data_fim)
SELECT d.id_dependente, u.id_usuario, '2019-01-01', '2021-01-05'
FROM Dependente d JOIN Usuario u ON u.login = '50000000485'
WHERE d.prec_cp = '45872213301'
  AND NOT EXISTS (SELECT 1 FROM ResponsavelLegal r
                  WHERE r.dependente_id = d.id_dependente AND r.usuario_id = u.id_usuario AND r.data_inicio = '2019-01-01');

-- Sofia: Marta e Renata, os dois responsaveis ativos
INSERT INTO ResponsavelLegal (dependente_id, usuario_id, data_inicio, data_fim)
SELECT d.id_dependente, u.id_usuario, '2017-02-01', NULL
FROM Dependente d JOIN Usuario u ON u.login = '70000000663'
WHERE d.prec_cp = '45810020002'
  AND NOT EXISTS (SELECT 1 FROM ResponsavelLegal r
                  WHERE r.dependente_id = d.id_dependente AND r.usuario_id = u.id_usuario AND r.data_inicio = '2017-02-01');

INSERT INTO ResponsavelLegal (dependente_id, usuario_id, data_inicio, data_fim)
SELECT d.id_dependente, u.id_usuario, '2017-02-01', NULL
FROM Dependente d JOIN Usuario u ON u.login = '60000000574'
WHERE d.prec_cp = '45810020002'
  AND NOT EXISTS (SELECT 1 FROM ResponsavelLegal r
                  WHERE r.dependente_id = d.id_dependente AND r.usuario_id = u.id_usuario AND r.data_inicio = '2017-02-01');
