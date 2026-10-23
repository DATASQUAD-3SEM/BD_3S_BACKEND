package fatec.fusex.nexus.common.exception;

public class MuitasTentativasException extends RuntimeException {

    public MuitasTentativasException() {
        super("Muitas tentativas com senha atual incorreta. Tente novamente em 15 minutos.");
    }
}