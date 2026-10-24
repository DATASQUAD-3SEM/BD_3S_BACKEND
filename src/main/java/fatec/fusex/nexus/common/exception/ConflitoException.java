package fatec.fusex.nexus.common.exception;

/** Use quando algo ja existe (ex: CPF ja cadastrado). Vira HTTP 409. */
public class ConflitoException extends RuntimeException {

    // Opcional: quando informado, o 409 aponta o campo em "detalhes" (igual ao 422)
    private final String campo;

    public ConflitoException(String mensagem) {
        this(null, mensagem);
    }

    public ConflitoException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String getCampo() {
        return campo;
    }
}
