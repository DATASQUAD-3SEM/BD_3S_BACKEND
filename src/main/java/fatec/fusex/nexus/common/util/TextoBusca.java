package fatec.fusex.nexus.common.util;

import java.util.Locale;

import fatec.fusex.nexus.common.exception.RegraDeNegocioException;

/**
 * Funcoes auxiliares para os parametros de busca recebidos pelos endpoints de listagem.
 *
 * As queries com LIKE usam {@code ESCAPE '!'}; por isso os curingas digitados pelo usuario
 * precisam ser escapados antes de montar o padrao de busca.
 */
public final class TextoBusca {

    private TextoBusca() {
    }

    /** Faz com que '%' e '_' digitados sejam tratados como texto comum (ESCAPE '!' na query). */
    public static String escaparCuringas(String termo) {
        return termo.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    /**
     * Quando o termo tem apenas digitos, pontos, hifens e espacos (CPF ou Prec CP digitado com mascara),
     * devolve so os digitos. Caso contrario, devolve texto vazio para indicar que a busca nao e numerica.
     */
    public static String digitosDaBusca(String termo) {
        if (termo.isEmpty() || !termo.matches("[\\d.\\-\\s]+")) {
            return "";
        }
        return termo.replaceAll("\\D", "");
    }

    /**
     * Converte o filtro de situacao das listagens do Chefe em valor do campo {@code ativo}:
     * ativos (padrao) devolve true, inativos devolve false e todos devolve null (sem filtro).
     * Qualquer outro valor gera 400.
     */
    public static Boolean filtroDeSituacao(String situacao) {
        if (situacao == null || situacao.isBlank()) {
            return true;
        }
        return switch (situacao.trim().toLowerCase(Locale.ROOT)) {
            case "ativos" -> true;
            case "inativos" -> false;
            case "todos" -> null;
            default -> throw new RegraDeNegocioException("situacao invalida: use ativos, inativos ou todos");
        };
    }
}
