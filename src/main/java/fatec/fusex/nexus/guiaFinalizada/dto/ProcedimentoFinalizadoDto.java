package fatec.fusex.nexus.guiaFinalizada.dto;

import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;

public record ProcedimentoFinalizadoDto(
        String codDgp,
        String descricao,
        Integer quantidade,
        Integer quantidadeAssinadas) {

    public static ProcedimentoFinalizadoDto fromEntity(GuiaProcedimento procedimento, long assinadas) {
        return new ProcedimentoFinalizadoDto(
                procedimento.getCodDgp(),
                procedimento.getDescricaoProcedimento(),
                procedimento.getQuantidadeAutorizada(),
                (int) assinadas);
    }
}