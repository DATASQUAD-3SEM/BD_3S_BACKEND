# Guia de Git (passo a passo)

`develop` = fonte unica da verdade. Ninguem faz commit direto nela: sempre via Pull Request (PR).

## Todo dia, ao comecar

```bash
git checkout develop
git pull origin develop
git checkout -b feature/scrum-30-post-pre-guia     # sua branch (nome = tipo/scrum-numero-descricao)
```

## Enquanto trabalha

```bash
git add .
git commit -m "SCRUM-30: cria POST /pre-guias"
git push -u origin feature/scrum-30-post-pre-guia
```

Ficou mais de 1 dia? Traga a develop para a sua branch para nao acumular conflito:

```bash
git pull origin develop
```

## Abrir o PR

No GitHub: "Compare & pull request" -> base = `develop`. Preencha o checklist. Marque 1 colega como revisor.

## Regras do time (aprovadas na reuniao)

1. `git pull origin develop` antes de comecar.
2. Branch vive no maximo 2 dias.
3. Codigo compartilhado vai para a `develop` **antes** de outros usarem.
4. Nao faca a mesma coisa que outro colega: veja o Jira antes.

## Definition of Ready (task pode entrar na Sprint)
- Todas as dependencias ja estao mergeadas na `develop`
- Criterios de aceite escritos
- Estimativa em story points

## Definition of Done (task pode sair da Sprint)
- Codigo mergeado na `develop`
- Revisado por 1 pessoa
- Testado localmente (back + front juntos)

## Deu conflito, e agora?
Nao entre em panico e **nao apague o codigo do colega**. Abra o arquivo, procure `<<<<<<<`, escolha/junte as duas versoes,
apague as marcas, `git add .`, `git commit`. Na duvida, chame o colega que mexeu no arquivo.
