package fatec.fusex.nexus.espelho.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;

public record EspelhoResponse(
        Long id,
        LocalDateTime dataInicio,
        LocalDateTime dataEnvio,
        List<ItemResponse> itens,
        BigDecimal valorTotal) {

    public static EspelhoResponse vazio() {
        return new EspelhoResponse(null, null, null, List.of(), BigDecimal.ZERO.setScale(2));
    }

    public static EspelhoResponse fromEntity(Espelho espelho, List<EspelhoItem> itens) {
        List<ItemResponse> linhas = itens.stream()
                .map(ItemResponse::fromEntity)
                .toList();
        BigDecimal total = linhas.stream()
                .map(ItemResponse::valorTotal)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        return new EspelhoResponse(
                espelho.getIdEspelho(),
                espelho.getDataCriacao(),
                espelho.getDataEnvio(),
                linhas,
                total);
    }

    public record ItemResponse(
            Long guiaId,
            String numeroGuia,
            String paciente,
            String codDgp,
            String descricao,
            Integer quantidadeSessoes,
            BigDecimal valorSessao,
            BigDecimal valorTotal) {

        public static ItemResponse fromEntity(EspelhoItem item) {
            GuiaProcedimento procedimento = item.getGuiaProcedimento();
            Guia guia = procedimento.getGuia();
            BigDecimal valorTotal = item.getValorSessao()
                    .multiply(BigDecimal.valueOf(item.getQuantidadeSessoes()))
                    .setScale(2);
            return new ItemResponse(
                    guia.getIdGuia(),
                    guia.getNumero(),
                    guia.getPreGuia().getBeneficiario().getNome(),
                    procedimento.getCodDgp(),
                    procedimento.getDescricaoProcedimento(),
                    item.getQuantidadeSessoes(),
                    item.getValorSessao(),
                    valorTotal);
        }
    }
}