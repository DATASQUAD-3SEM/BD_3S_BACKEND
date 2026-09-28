# Guia do Banco de Dados (Flyway)

O banco e criado **so** pelos arquivos SQL em `src/main/resources/db/migration`.
O Hibernate nao cria nem altera tabela (`ddl-auto=none`).

## Regras
1. **Nunca edite** uma migration que ja foi para a develop (`V1__...`). O Flyway guarda o "checksum" e vai reclamar.
2. Para mudar o banco, crie **arquivo novo**.
3. Nome do arquivo: `V` + **data e hora** + `__` + descricao (dois underscores!):

```
V202609251430__adiciona_status_em_ocs.sql
```
   Usamos data/hora (AAAAMMDDHHMM) em vez de 2, 3, 4... porque se duas pessoas criarem `V2__` no mesmo dia, quebra.
4. Escreva SQL que funcione em **MySQL e H2**: tipos simples (`BIGINT`, `VARCHAR`, `INT`, `DATE`, `TIMESTAMP`), sem `ENGINE=`, sem `JSON`, sem `ENUM`.
5. Mudou a migration? Mude tambem a entidade Java correspondente (e vice-versa) **no mesmo PR**.
6. Rode `./mvnw test`: o `NexusApplicationTests` aplica todas as migrations no H2 e falha se algo estiver errado.

## Dados de exemplo
`db/seed/R__dados_exemplo.sql` cria 2 OCS e 3 exames de exemplo (dev, h2 e testes). Nao vai para producao.
Precisa de mais dado de exemplo? Edite esse arquivo (sempre com `INSERT ... WHERE NOT EXISTS`).

## Recomecar o MySQL do zero
```bash
docker compose down -v
docker compose up -d
```

## Tabelas atuais
`procedimento_exame`, `ocs`, `ocs_procedimento`, `beneficiario`, `pre_guia`, `pre_guia_procedimento`
