package fatec.fusex.nexus.common.exception;

/**
 * Erro 422: o formato da requisicao esta correto, mas um campo quebra uma regra de validacao.
 * O nome do campo e devolvido para que o frontend mostre o erro no lugar certo.
 */
public class DadosInvalidosException extends RuntimeException {

    private final String campo;

    public DadosInvalidosException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}