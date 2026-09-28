package fatec.fusex.nexus.ocs.dto;

import fatec.fusex.nexus.procedimento.ProcedimentoExame;

/** DTO de saida dos procedimentos/exames de uma OCS. */
public record ProcedimentoExameResponse(
        Long id,
        String codigoTuss,
        String terminologiaProcedimentoEvento,
        String rolAnsResolucaoNormativa,
        String rolAns,
        String grupo,
        String subgrupo,
        String capitulo) {

    public static ProcedimentoExameResponse de(ProcedimentoExame p) {
        return new ProcedimentoExameResponse(
                p.getId(),
                p.getCodigoTuss(),
                p.getTerminologiaProcedimentoEvento(),
                p.getRolAnsResolucaoNormativa(),
                p.getRolAns(),
                p.getGrupo(),
                p.getSubgrupo(),
                p.getCapitulo());
    }
}