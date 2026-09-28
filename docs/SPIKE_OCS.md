# Spike: Fonte das OCS

## Pergunta

De onde virão os dados das OCS utilizadas pelo sistema?

## Decisão

As OCS serão provenientes de um cadastro interno no banco de dados do sistema.

## Impacto no projeto

As OCS serão consultadas a partir da estrutura de banco de dados existente no backend.

O backend disponibilizará endpoints para listar as OCS cadastradas e consultar os procedimentos vinculados a uma OCS.

## Próximo passo

Implementar a SCRUM-29b:

- GET /ocs
- GET /ocs/{id}/procedimentos