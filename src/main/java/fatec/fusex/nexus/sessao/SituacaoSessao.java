package fatec.fusex.nexus.sessao;

/**
 * Situacao de uma sessao (modelo logico, secao 2.4). ASSINADA nao volta para outro status.
 */
public enum SituacaoSessao {
    DISPONIVEL, SOLICITADA, ASSINADA, EXPIRADA
}
