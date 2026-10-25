package fatec.fusex.nexus.correcaoMedico.dto;

import fatec.fusex.nexus.procedimentoExame.ProcedimentoExame;

/** Item do GET /medicos/me/procedimentos (T14-BE4): mesmo formato do T2-BE3, com DTO proprio. */
public record ProcedimentoCorrecaoResponse(String codigoTuss, String nome) {

    public static ProcedimentoCorrecaoResponse de(ProcedimentoExame p) {
        return new ProcedimentoCorrecaoResponse(p.getCodigoTuss(), p.getTerminologiaProcedimento());
    }
}
