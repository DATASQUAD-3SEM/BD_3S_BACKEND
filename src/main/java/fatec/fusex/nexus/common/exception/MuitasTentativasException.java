package fatec.fusex.nexus.common.exception;

/**
 * Erro 429: tentativas demais em pouco tempo (ex.: senha atual errada 5 vezes seguidas, T13-BE1).
 */
public class MuitasTentativasException extends RuntimeException {

    public MuitasTentativasException() {
        super("Muitas tentativas com senha atual incorreta. Tente novamente em 15 minutos.");
    }
}