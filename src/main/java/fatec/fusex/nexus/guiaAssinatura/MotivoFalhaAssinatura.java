package fatec.fusex.nexus.guiaAssinatura;

public enum MotivoFalhaAssinatura {
    GUIA_NAO_ENCONTRADA("guia_nao_encontrada"),
    JA_ASSINADA_PELO_USUARIO("ja_assinada_pelo_usuario"),
    JA_ASSINADA_POR_MEDICO("ja_assinada_por_medico"),
    JA_ASSINADA_PELO_CHEFE("ja_assinada_pelo_chefe"),
    CONCLUIDA("concluida");

    private final String codigo;

    MotivoFalhaAssinatura(String codigo) {
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }
}