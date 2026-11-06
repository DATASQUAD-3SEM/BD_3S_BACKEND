package fatec.fusex.nexus.common.exception;

/**
 * Erro 429 generico: o usuario fez requisicoes demais em pouco tempo (ex.: leitura de PDF da Guia, T11-BE1).
 */
public class LimiteRequisicoesException extends RuntimeException {

    public LimiteRequisicoesException(String mensagem) {
        super(mensagem);
    }
}
