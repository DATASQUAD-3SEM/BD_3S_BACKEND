package fatec.fusex.nexus.common.exception;

/** Use quando o pedido viola uma regra do FUSEX (ex: pre-guia sem encaminhamento). Vira HTTP 400. */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
