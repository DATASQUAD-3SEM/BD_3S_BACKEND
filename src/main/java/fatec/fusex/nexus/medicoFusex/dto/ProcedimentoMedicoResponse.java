package fatec.fusex.nexus.medicoFusex.dto;

import fatec.fusex.nexus.procedimentoExame.ProcedimentoExame;

public record ProcedimentoMedicoResponse(
        String codigoTuss,
        String nome) {

    public static ProcedimentoMedicoResponse de(ProcedimentoExame p) {
        return new ProcedimentoMedicoResponse(
                p.getCodigoTuss(),
                p.getTerminologiaProcedimento());
    }
}