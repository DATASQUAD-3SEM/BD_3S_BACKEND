package fatec.fusex.nexus.common.exception;

/** Use quando algo nao existe no banco. Vira HTTP 404. Ex: throw new RecursoNaoEncontradoException("OCS nao encontrada"); */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
