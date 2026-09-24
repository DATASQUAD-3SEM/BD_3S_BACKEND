# Arquitetura

## Organizacao: por FEATURE, nao por camada

Cada pasta (`preguia`, `ocs`, ...) guarda tudo de um assunto. Isso e proposital: quem e dono de uma
feature mexe nos proprios arquivos e ninguem colide (problema P1/P6 da reuniao).

```
preguia/
  PreGuia.java              <- entidade (JA EXISTE, base compartilhada)
  PreGuiaRepository.java    <- JA EXISTE
  PreGuiaController.java    <- dono da feature cria
  PreGuiaService.java       <- dono da feature cria
  dto/...                   <- dono da feature cria (o que entra/sai na API)
```

## Camadas (sempre nesta ordem)

`Controller` (recebe HTTP) -> `Service` (regras do FUSEX) -> `Repository` (banco)

- Controller: nunca acessa Repository direto e nunca tem regra de negocio.
- Service: aqui ficam as regras (ex: "nao cria pre-guia sem arquivo de encaminhamento").
- Nunca devolva a entidade (`PreGuia`) direto na API: crie um DTO (`record`). Nunca exponha `senha`.
- Para erro, **lance a excecao** (nao use try/catch no controller):

| Situacao | Lance | HTTP |
|---|---|---|
| Nao achou no banco | `RecursoNaoEncontradoException` | 404 |
| Violou regra do FUSEX | `RegraDeNegocioException` | 400 |
| Ja existe (CPF repetido) | `ConflitoException` | 409 |

## O que e COMPARTILHADO (ja pronto na develop - nao recrie)

| Arquivo | Serve para |
|---|---|
| `Beneficiario`, `Ocs`, `ProcedimentoExame`, `PreGuia` (+ Repositories) | Espelho das tabelas |
| `StatusPreGuia` | Status da pre-guia (enum) |
| `common/exception/*` | Erros padronizados em JSON |
| `common/config/CorsConfig` | Libera o front em `localhost:5173` |
| `common/config/PasswordConfig` | `PasswordEncoder` (BCrypt) para senhas |
| `common/storage/ArmazenamentoArquivo` | Salvar/remover arquivo (encaminhamento) |
| `OcsService` | Exemplo do padrao Service + teste Mockito |

## Onde cada task do Jira encaixa (apos a reorganizacao do PDF)

| Task | Onde fica |
|---|---|
| 22 - vinculo PreGuia <-> beneficiario | `preguia/PreGuiaService` (a relacao ja existe na entidade) |
| 27 - campo do arquivo na PreGuia | **Ja pronto**: `PreGuia.encaminhamentoUrl` |
| 30 - `POST /pre-guias` multipart | `preguia/PreGuiaController` + `PreGuiaService`, usando `ArmazenamentoArquivo` |
| 28 - bloquear sem arquivo | `PreGuiaService` -> `RegraDeNegocioException` |
| 31 - validar procedimentos | `PreGuiaService`: cada procedimento tem que estar em `ocs.getProcedimentos()` |
| 32 - status inicial | **Ja pronto**: nasce `RASCUNHO` (confirmar com PO) |
| 19 - storage (spike) | Decide; hoje existe `ArmazenamentoLocal` provisorio |
| 29 - OCS (spike + endpoint) | `ocs/OcsController` usando `OcsService` |
| 33 a 38 - telas | Front (repo do front) - componentes separados |

## Testes

- Regra de negocio (Service): **JUnit + Mockito** - copie `OcsServiceTest`.
- Banco/mapeamento: `@SpringBootTest` + `@ActiveProfiles("test")` + `@Transactional` - copie `BeneficiarioRepositoryTest`.
- Sem Spring (rapido): copie `ArmazenamentoLocalTest`.
- Nome do teste: `metodo_situacao_resultado` (ex.: `buscarPorId_quandoNaoExiste_lancaExcecao`).
