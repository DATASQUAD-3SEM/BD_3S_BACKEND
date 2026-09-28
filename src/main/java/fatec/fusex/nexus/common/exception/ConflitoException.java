package fatec.fusex.nexus.common.exception;

/** Use quando algo ja existe (ex: CPF ja cadastrado). Vira HTTP 409. */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
