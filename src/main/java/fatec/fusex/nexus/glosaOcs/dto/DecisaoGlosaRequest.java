package fatec.fusex.nexus.glosaOcs.dto;

import java.util.List;

/** Decisao da OCS por Guia glosada (T27-BE2). Vale para todas as linhas da Guia na rodada. */
public record DecisaoGlosaRequest(List<DecisaoGuiaRequest> decisoes, String responsavel) {

    public record DecisaoGuiaRequest(Long guiaId, String decisao, String motivo) {
    }
}
