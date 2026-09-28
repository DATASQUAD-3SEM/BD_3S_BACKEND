# Decisoes pendentes e pontos de atencao

Coisas que **eu (quem montou a base) precisei decidir ou encontrei inconsistentes** entre `manual.txt`, `modelo_logico.txt`
e o PDF de pautas. Levem para o PO / reuniao. Ate la, o que esta no codigo e provisorio.

## Divergencias encontradas

| # | Assunto | O que a base fez | Precisa confirmar |
|---|---|---|---|
| 1 | **Tabela `pre_guia_procedimento`** | Foi **adicionada**. O modelo logico nao liga PreGuia a procedimentos, mas a US2 e a task 31 dizem que a pre-guia tem "os procedimentos/exames". | Aprovar / atualizar o modelo logico |
| 2 | **Nome da coluna do arquivo** | Seguiu o modelo logico: `encaminhamento_url` (Java: `encaminhamentoUrl`). O PDF fala em `arquivo_encaminhamento_url`. | Escolher **um** nome; se mudar, criar migration de rename |
| 3 | **Status da pre-guia** | Enum proposto: `RASCUNHO`, `PENDENTE`, `APROVADA`, `REJEITADA` (o PDF cita "pre-guia em rascunho"; o manual cita "analisada e aprovada"). | Confirmar valores e o status inicial (task 32) |
| 4 | **Colunas obrigatorias** | O modelo so marca `NOT NULL` em poucos campos. Alem deles, tornei obrigatorios os obviamente essenciais: `nome`/`senha` (beneficiario), `nome`/`contrato_num` (ocs), `codigo_tuss`/terminologia (procedimento), `status`/`data_emissao` (pre_guia). | Revisar |
| 5 | **Tipos e tamanhos** | Escolhidos por mim: CPF `VARCHAR(11)` (so digitos), `prec_cp` `VARCHAR(20)`, telefone `VARCHAR(20)`. | Confirmar formato do Prec-CP |
| 6 | **Dados de exemplo** | Codigos TUSS do seed sao de exemplo, nao dados oficiais. | Carga real do catalogo TUSS/ANS |

## Fora do escopo da base (ainda nao existe)

- **Perfis de usuario**: o manual fala em medico do FUSEX (que pode gerar pre-guia) e funcionario do FUSEX (que aprova). O modelo so tem `beneficiario`. Falta definir tabela/perfis.
- **Login / autenticacao (JWT ou sessao)**: so existe o `PasswordEncoder`. Hoje **nenhum endpoint e protegido**.
- **Assinatura digital** no cadastro (informacao do cliente): sem modelo definido.
- **Guia, espelho, fatura, lisura, glosa, mapa, liquidacao**: sprints futuras.
- **Storage (spike SCRUM 19)**: `ArmazenamentoLocal` grava em disco (`./uploads`). Serve para desenvolver; para producao decidir (disco do servidor, S3...).
- **Cadastro da OCS (spike SCRUM 29)**: hoje as OCS entram via seed/SQL; falta decidir se e cadastro proprio ou externo.

## Observacao sobre o PDF de pautas
A pagina 2 do PDF (problemas P3 a P5) nao veio nos anexos; a base seguiu P1, P2 e P6 e a tabela final das tasks.

## Aviso sobre a verificacao
O projeto **nao foi compilado nem testado** quando esta base foi gerada (o ambiente nao tinha acesso ao Maven Central).
A primeira pessoa a rodar `./mvnw test` deve avisar o grupo do resultado antes de subir para a `develop`.
Se aparecer erro de compilacao/dependencia, corrija em um PR unico.
