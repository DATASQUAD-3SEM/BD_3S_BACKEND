package fatec.fusex.nexus.medicoFusex.dto;

import fatec.fusex.nexus.procedimentoExame.ProcedimentoExame;

/**
 * Item da busca de procedimentos do medico (T2-BE3): codigo TUSS e nome.
 */
public record ProcedimentoMedicoResponse(
        String codigoTuss,
        String nome) {

    public static ProcedimentoMedicoResponse de(ProcedimentoExame p) {
        return new ProcedimentoMedicoResponse(
                p.getCodigoTuss(),
                p.getTerminologiaProcedimento());
    }
}