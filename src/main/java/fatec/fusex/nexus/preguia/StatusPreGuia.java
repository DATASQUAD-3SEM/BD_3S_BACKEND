package fatec.fusex.nexus.preguia;

/**
 * Situacao da pre-guia. PROPOSTA inicial (confirmar com o PO, veja docs/DECISOES_PENDENTES.md).
 * No banco e gravado como texto (ex: "PENDENTE").
 */
public enum StatusPreGuia {
    /** Beneficiario ainda esta montando (pode reenviar o arquivo do encaminhamento). */
    RASCUNHO,
    /** Enviada ao setor de guias do FUSEX, aguardando analise. */
    PENDENTE,
    /** Aprovada pelo funcionario do FUSEX (a Guia pode ser gerada). */
    APROVADA,
    /** Recusada pelo funcionario do FUSEX. */
    REJEITADA
}
