package fatec.fusex.nexus.glosaOcs.dto;

import java.math.BigDecimal;

import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;
import fatec.fusex.nexus.setorFatura.ControleFluxoFatura;

/** Linha com glosa, com a decisao da OCS (T27-BE1). */
public record GlosaGuiaResponse(
        Long itemId,
        Long guiaId,
        String guiaNumero,
        String paciente,
        String codigo,
        String procedimento,
        Integer quantidadeSessoes,
        BigDecimal valorSessao,
        BigDecimal valorContrato,
        String origemContrato,
        String observacao,
        String decisaoOcs,
        String motivoContestacaoOcs,
        BigDecimal glosa) {

    public static GlosaGuiaResponse fromEntity(EspelhoItem item) {
        GuiaProcedimento procedimento = item.getGuiaProcedimento();
        Guia guia = procedimento.getGuia();
        return new GlosaGuiaResponse(
                item.getIdEspelhoItem(),
                guia.getIdGuia(),
                guia.getNumero(),
                guia.getPreGuia().getBeneficiario().getNome(),
                procedimento.getCodDgp(),
                procedimento.getDescricaoProcedimento(),
                item.getQuantidadeSessoes(),
                item.getValorSessao(),
                item.getValorContratoSessao(),
                item.getOrigemContrato() == null ? null : item.getOrigemContrato().name(),
                item.getObservacao(),
                item.getDecisaoOcs(),
                item.getMotivoContestacaoOcs(),
                ControleFluxoFatura.glosaDaLinha(item));
    }
}
