package fatec.fusex.nexus.common.exception;

/**
 * Erro 401: nenhum usuario logado, ou o login informado nao corresponde a nenhuma conta.
 */
public class NaoAutenticadoException extends RuntimeException {
    public NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }
}