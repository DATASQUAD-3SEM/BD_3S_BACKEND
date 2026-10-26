package fatec.fusex.nexus.preGuia;

/**
 * Status da pre-guia. Fluxo: PENDENTE -> EM_ANALISE (ao abrir) -> ATENCAO -> PENDENTE (reenvio) ... -> CONCLUIDA.
 * CONCLUIDA so e atingido depois das duas assinaturas da Guia (medico e chefe).
 */
public enum PreGuiaStatus {
    PENDENTE, EM_ANALISE, ATENCAO, CONCLUIDA
}