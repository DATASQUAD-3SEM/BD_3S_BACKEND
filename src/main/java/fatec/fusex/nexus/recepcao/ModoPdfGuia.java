package fatec.fusex.nexus.recepcao;

import fatec.fusex.nexus.common.exception.RegraDeNegocioException;

public enum ModoPdfGuia {

    VISUALIZAR(false),
    IMPRIMIR(false),
    BAIXAR(true);

    private final boolean anexo;

    ModoPdfGuia(boolean anexo) {
        this.anexo = anexo;
    }

    public boolean isAnexo() {
        return anexo;
    }

    public static ModoPdfGuia de(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new RegraDeNegocioException("Informe o modo: visualizar, imprimir ou baixar");
        }
        for (ModoPdfGuia modo : values()) {
            if (modo.name().equalsIgnoreCase(valor.trim())) {
                return modo;
            }
        }
        throw new RegraDeNegocioException("Modo invalido: use visualizar, imprimir ou baixar");
    }
}