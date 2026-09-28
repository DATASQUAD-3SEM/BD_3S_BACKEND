# Spike: Fonte das OCS

## Pergunta
De onde virão os dados das OCS utilizadas pelo sistema?

## Opções avaliadas
| Opção | Prós | Contras |
|---|---|---|
| Cadastro próprio no banco do Nexus | Já temos a tabela `ocs` + `ocs_procedimento` no modelo lógico. Controle total do fluxo. Seed cobre dev/testes. | Exige tela/rotina de cadastro (sprint futura). |
| Integração externa (API do Exército) | Dados sempre atualizados. | Não existe contrato/credencial definido. Bloqueia a sprint. |
| Planilha importada manualmente | Simples no início. | Duplicação de dados, sem rastreio. |

## Decisão
As OCS serão provenientes de **cadastro interno no banco de dados** do sistema
(tabelas `ocs` e `ocs_procedimento`, já existentes na migration V1).

## Impacto no projeto
- Backend expõe `GET /ocs` e `GET /ocs/{id}/procedimentos`.
- O front consome esses endpoints em `SelecaoOcs` (SCRUM 35) e `SelecaoProcedimentos` (SCRUM 36).
- Cadastro de OCS via tela fica para sprint futura; hoje entra via seed/SQL.

## Próximo passo (SCRUM 29b)
- `GET /ocs` → lista todas as OCS
- `GET /ocs/{id}/procedimentos` → procedimentos vinculados a uma OCS