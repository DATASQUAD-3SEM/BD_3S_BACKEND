package fatec.fusex.nexus.common.exception;

/** Use quando o usuario nao tem perfil para a operacao (ex: nao e medico). Vira HTTP 403. */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }
}
