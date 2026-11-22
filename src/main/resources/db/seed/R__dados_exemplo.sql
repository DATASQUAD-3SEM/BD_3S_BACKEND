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

INSERT INTO Usuario (login, senha_hash, perfil)
SELECT 'setor.fatura', '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO', 'SETOR_FATURA' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = 'setor.fatura');

INSERT INTO Usuario (login, senha_hash, perfil)
SELECT 'rastreabilidade', '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO', 'RASTREABILIDADE' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = 'rastreabilidade');

INSERT INTO Usuario (login, senha_hash, perfil)
SELECT 'auditoria', '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO', 'AUDITORIA' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = 'auditoria');

-- Contas criadas por versoes antigas deste seed ficaram com o placeholder.
-- Este UPDATE corrige so essas linhas; senhas ja trocadas pelos usuarios nao sao tocadas.
UPDATE Usuario SET senha_hash = '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO'
WHERE senha_hash = 'HASH_BCRYPT_AQUI';
-- ---------------------------------------------------------------------
-- T29-BE2 (SCRUM-251): dependentes de exemplo, conforme modelo 2.7 e regra 13.
-- Casos: menor sob a guarda do titular, menor sob responsavel externo (sem Prec CP), maior vitalicio e maior com
-- vigencia determinada (ambos com conta propria) e um dependente com vigencia vencida.
-- Responsavel externo: conta propria pelo CPF, perfil RESPONSAVEL_EXTERNO, sem Prec CP.
-- ---------------------------------------------------------------------
-- CPFs com digitos verificadores validos (o cadastro e a consulta de conta pelo CPF recusam CPF invalido).
-- Bancos criados antes desta correcao tinham Helena com 41282945067 e Sofia com 39053344806: os UPDATEs abaixo
-- trocam so essas duas linhas, mantendo senha, assinatura, pre-guias e Guias.
UPDATE Usuario SET login = '41282945009' WHERE login = '41282945067' AND perfil = 'RESPONSAVEL_EXTERNO';
UPDATE Dependente SET cpf = '39053344888' WHERE cpf = '39053344806' AND prec_cp = '12345678903';

INSERT INTO Usuario (login, senha_hash, perfil)
SELECT '41282945009', '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO', 'RESPONSAVEL_EXTERNO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = '41282945009');

INSERT INTO ResponsavelExterno (usuario_id, nome, telefone)
SELECT u.id_usuario, 'Helena Costa', '12998765432'
FROM Usuario u
WHERE u.login = '41282945009'
  AND NOT EXISTS (SELECT 1 FROM ResponsavelExterno r WHERE r.usuario_id = u.id_usuario);

-- Maior de idade com conta propria (login pelo CPF, perfil BENEFICIARIO), sem linha em Beneficiario.
INSERT INTO Usuario (login, senha_hash, perfil)
SELECT '98765432100', '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO', 'BENEFICIARIO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = '98765432100');

-- Miguel: menor sob a guarda do titular Antonio (MENOR_TITULAR).
INSERT INTO Dependente (titular_id, usuario_id, responsavel_id, nome, data_nascimento, cpf, telefone,
                        prec_cp, vinculo, tipo, vigencia_fim)
SELECT b.usuario_id, NULL, NULL, 'Miguel Ferreira', '2026-03-21', '39053344705', NULL,
       '12345678902', 'Filho', 'MENOR_TITULAR', '2048-03-20'
FROM Beneficiario b
JOIN Usuario t ON t.id_usuario = b.usuario_id
WHERE t.login = '52998224725'
  AND NOT EXISTS (SELECT 1 FROM Dependente d WHERE d.prec_cp = '12345678902');

-- Sofia: menor sob a guarda da responsavel externa Helena (MENOR_RESPONSAVEL), titular Antonio.
INSERT INTO Dependente (titular_id, usuario_id, responsavel_id, nome, data_nascimento, cpf, telefone,
                        prec_cp, vinculo, tipo, vigencia_fim)
SELECT b.usuario_id, NULL, r.id_usuario, 'Sofia Ferreira', '2017-05-15', '39053344888', NULL,
       '12345678903', 'Filha', 'MENOR_RESPONSAVEL', '2039-05-14'
FROM Beneficiario b
JOIN Usuario t ON t.id_usuario = b.usuario_id
JOIN Usuario r ON r.login = '41282945009'
WHERE t.login = '52998224725'
  AND NOT EXISTS (SELECT 1 FROM Dependente d WHERE d.prec_cp = '12345678903');

-- Carla: maior de idade, conta propria, vigencia vitalicia (tipo MAIOR).
INSERT INTO Dependente (titular_id, usuario_id, responsavel_id, nome, data_nascimento, cpf, telefone,
                        prec_cp, vinculo, tipo, vigencia_fim)
SELECT b.usuario_id, u.id_usuario, NULL, 'Carla Ferreira', '1985-02-08', '98765432100', NULL,
       '12345678901', 'Cônjuge', 'MAIOR', NULL
FROM Beneficiario b
JOIN Usuario t ON t.id_usuario = b.usuario_id
JOIN Usuario u ON u.login = '98765432100'
WHERE t.login = '52998224725'
  AND NOT EXISTS (SELECT 1 FROM Dependente d WHERE d.prec_cp = '12345678901');

-- Pedro: maior de idade, conta propria, vigencia determinada (tipo MAIOR).
INSERT INTO Usuario (login, senha_hash, perfil)
SELECT '41827365080', '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO', 'BENEFICIARIO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = '41827365080');

INSERT INTO Dependente (titular_id, usuario_id, responsavel_id, nome, data_nascimento, cpf, telefone,
                        prec_cp, vinculo, tipo, vigencia_fim)
SELECT b.usuario_id, u.id_usuario, NULL, 'Pedro Ferreira', '2004-08-10', '41827365080', NULL,
       '12345678904', 'Filho', 'MAIOR', '2030-08-09'
FROM Beneficiario b
JOIN Usuario t ON t.id_usuario = b.usuario_id
JOIN Usuario u ON u.login = '41827365080'
WHERE t.login = '52998224725'
  AND NOT EXISTS (SELECT 1 FROM Dependente d WHERE d.prec_cp = '12345678904');

-- Rita: vigencia vencida (tipo MAIOR, conta propria). Nao aparece em Nova pre-guia nem na busca do medico.
INSERT INTO Usuario (login, senha_hash, perfil)
SELECT '52736481909', '$2a$10$HejWOVIFVuEHjfYru8lEcexIDdtnkDhjKbbX7rR6.wysdw4BNrwXO', 'BENEFICIARIO' FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM Usuario WHERE login = '52736481909');

INSERT INTO Dependente (titular_id, usuario_id, responsavel_id, nome, data_nascimento, cpf, telefone,
                        prec_cp, vinculo, tipo, vigencia_fim)
SELECT b.usuario_id, u.id_usuario, NULL, 'Rita Ferreira', '2001-01-20', '52736481909', NULL,
       '12345678905', 'Filha', 'MAIOR', '2025-01-19'
FROM Beneficiario b
JOIN Usuario t ON t.id_usuario = b.usuario_id
JOIN Usuario u ON u.login = '52736481909'
WHERE t.login = '52998224725'
  AND NOT EXISTS (SELECT 1 FROM Dependente d WHERE d.prec_cp = '12345678905');
