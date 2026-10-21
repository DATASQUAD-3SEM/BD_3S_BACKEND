package fatec.fusex.nexus.procedimentoExame.dto;

import fatec.fusex.nexus.procedimentoExame.ProcedimentoExame;

/** DTO de saida de procedimento/exame. */
public record ProcedimentoExameResponse(
        Long id,
        String codigoTuss,
        String terminologiaProcedimento,
        String rolAns,
        String rolAnsResolucaoNormativa,
        String subgrupo) {

    public static ProcedimentoExameResponse de(ProcedimentoExame p) {
        return new ProcedimentoExameResponse(
                p.getIdProcedimentoExame(),
                p.getCodigoTuss(),
                p.getTerminologiaProcedimento(),
                p.getRolAns().name(),
                p.getRolAnsResolucaoNormativa(),
                p.getSubgrupo());
    }
}