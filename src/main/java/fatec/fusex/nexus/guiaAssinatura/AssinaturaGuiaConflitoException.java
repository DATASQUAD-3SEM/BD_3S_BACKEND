package fatec.fusex.nexus.guiaAssinatura;

import fatec.fusex.nexus.common.exception.ConflitoException;

public class AssinaturaGuiaConflitoException extends ConflitoException {

    private final MotivoFalhaAssinatura motivo;

    public AssinaturaGuiaConflitoException(MotivoFalhaAssinatura motivo, String mensagem) {
        super(mensagem);
        this.motivo = motivo;
    }

    public MotivoFalhaAssinatura getMotivo() {
        return motivo;
    }
}