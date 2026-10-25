package fatec.fusex.nexus.assinaturaGuia;

/**
 * Papel em que uma assinatura foi feita na Guia.
 * O papel nunca vem do corpo da requisicao: ele e derivado do perfil do usuario logado (MEDICO ou CHEFE_FUSEX).
 */
public enum AssinaturaGuiaPapel {
    CHEFE, MEDICO
}
