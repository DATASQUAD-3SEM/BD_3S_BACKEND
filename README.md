# Nexus - Backend

API do Nexus: aplicativo que facilita o fluxo de trabalho do FUSEX
(pre-guia, guia, lisura, glosa). Front-end em repositorio separado (Vite + React + TypeScript).

**Stack:** Java 21 - Spring Boot 4 - MySQL 8 - Flyway - H2 (testes) - JUnit 5 + Mockito - Maven

---

## 1. O que instalar (uma vez so)

| Programa | Para que | Como conferir |
|---|---|---|
| **JDK 21** | rodar o Java | `java -version` mostra 21 |
| **Git** | versionamento | `git --version` |
| **IntelliJ IDEA Community** (ou VS Code + Extension Pack for Java) | editar codigo | - |
| **Docker Desktop** *(opcional)* | MySQL sem instalar nada | `docker --version` |

Maven **nao precisa instalar**: o projeto traz o `mvnw`.

## 2. Rodar pela primeira vez

```bash
git clone <URL-DO-REPOSITORIO>
cd nexus
git checkout develop
```

### Opcao A - Sem instalar banco (mais facil)

Usa H2 (banco na memoria; os dados somem ao parar).

```bash
# Mac/Linux
./mvnw spring-boot:run -Dspring-boot.run.profiles=h2
# Windows (PowerShell/CMD)
mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
```

### Opcao B - Com MySQL (igual a producao)

```bash
docker compose up -d          # sobe o MySQL (usuario/senha/banco: nexus)
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

### Deu certo?

Abra <http://localhost:8080/actuator/health> - deve aparecer `{"status":"UP"}`.

## 3. Rodar os testes

```bash
./mvnw test          # Windows: mvnw.cmd test
```

Rode **sempre antes de abrir um PR**. Tem que terminar com `BUILD SUCCESS`.

## 4. Onde fica cada coisa

Leia `docs/ARQUITETURA.md` (2 minutos). Resumo:

```
src/main/java/fatec/fusex/nexus/
  common/          -> COMPARTILHADO: erros, CORS, senha, upload de arquivo
  beneficiario/    -> entidade + repository (+ o que a feature precisar)
  ocs/             -> idem (ja tem OcsService de exemplo)
  procedimento/    -> idem
  preguia/         -> idem
src/main/resources/
  db/migration/    -> tabelas (Flyway)
  db/seed/         -> dados de exemplo (so dev/h2/teste)
src/test/java/...  -> testes (mesma estrutura de pastas)
docs/              -> guias do time
```

## 5. As 6 regras que evitam 90% dos problemas

1. **`git pull origin develop` antes de comecar a mexer.** Todo dia.
2. **Antes de criar uma classe, procure se ela ja existe** (Ctrl+Shift+F / Ctrl+N). Duas classes com o mesmo nome e conteudo diferente e o nosso maior risco.
3. **Nunca edite** `Beneficiario`, `Ocs`, `PreGuia`, `ProcedimentoExame`, migrations antigas ou a pasta `common/` sem avisar o grupo. Precisa mudar? Abra PR pequeno so com essa mudanca.
4. **Cada pessoa e dona de uma feature inteira** (controller + service + DTO + teste), nao de uma camada.
5. **Branch dura no maximo 2 dias.** Abra PR cedo, mesmo incompleto.
6. **Nenhum PR entra sem 1 revisor e sem `./mvnw test` passando.**

Detalhes: `docs/GUIA_GIT.md` - `docs/GUIA_BANCO_DE_DADOS.md` - `docs/DECISOES_PENDENTES.md`

## 6. Problemas comuns

| Erro | O que fazer |
|---|---|
| `Port 8080 already in use` | Tem outro Nexus rodando. Feche-o. |
| `Communications link failure` / nao conecta no MySQL | Rode `docker compose up -d` e espere 20s. Ou use a Opcao A (h2). |
| `Migration checksum mismatch` | Alguem editou uma migration antiga. `docker compose down -v` e suba de novo; e avise o grupo. |
| `LazyInitializationException` | Voce acessou `getOcs()`/`getProcedimentos()` fora de uma transacao. Coloque `@Transactional` no metodo do service. |
| `release version 21 not supported` | Seu JDK nao e 21. Instale o JDK 21 e configure a IDE. |
