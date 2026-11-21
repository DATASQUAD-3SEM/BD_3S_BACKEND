package fatec.fusex.nexus.setorFatura.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.guia.Guia;
import fatec.fusex.nexus.guiaProcedimento.GuiaProcedimento;
import fatec.fusex.nexus.setorFatura.ControleFluxoFatura;
import fatec.fusex.nexus.setorFatura.EtapaFluxo;
import fatec.fusex.nexus.setorFatura.FaturaEvento;

public record FaturaDetalheResponse(
        Long id,
        String ocs,
        LocalDateTime dataInicio,
        LocalDateTime dataEnvio,
        LocalDateTime dataEntrada,
        String situacao,
        String numeroFatura,
        BigDecimal valorTotal,
        List<LinhaResponse> linhas,
        TotaisResponse totais,
        List<EventoResponse> eventos,
        List<EtapaResponse> controleFluxo) {

    public static FaturaDetalheResponse fromEntity(Espelho espelho, List<EspelhoItem> itens,
                                                   List<FaturaEvento> eventos, List<EtapaFluxo> etapas) {
        BigDecimal informado = ControleFluxoFatura.valorInformado(itens).setScale(2);
        BigDecimal valorFinal = ControleFluxoFatura.valorFinal(itens);
        BigDecimal glosa = valorFinal == null ? null : informado.subtract(valorFinal).setScale(2);
        return new FaturaDetalheResponse(
                espelho.getIdEspelho(),
                espelho.getOcs().getNome(),
                espelho.getDataCriacao(),
                espelho.getDataEnvio(),
                espelho.getDataEnvio(),
                espelho.getSituacaoFatura().name(),
                espelho.getNumeroFatura(),
                informado,
                itens.stream().map(LinhaResponse::fromEntity).toList(),
                new TotaisResponse(informado, glosa, valorFinal),
                eventos.stream().map(EventoResponse::fromEntity).toList(),
                etapas.stream().map(EtapaResponse::fromEntity).toList());
    }

    public record LinhaResponse(
            Long itemId,
            Long guiaId,
            String guiaNumero,
            String paciente,
            String codigo,
            String procedimento,
            Integer quantidadeSessoes,
            BigDecimal valorSessao,
            BigDecimal valorTotal,
            BigDecimal valorContrato,
            String origemContrato,
            String observacao,
            BigDecimal glosa,
            String comparacao) {

        public static LinhaResponse fromEntity(EspelhoItem item) {
            GuiaProcedimento procedimento = item.getGuiaProcedimento();
            Guia guia = procedimento.getGuia();
            BigDecimal valorTotal = item.getValorSessao()
                    .multiply(BigDecimal.valueOf(item.getQuantidadeSessoes()))
                    .setScale(2);
            BigDecimal contrato = item.getValorContratoSessao();
            return new LinhaResponse(
                    item.getIdEspelhoItem(),
                    guia.getIdGuia(),
                    guia.getNumero(),
                    guia.getPreGuia().getBeneficiario().getNome(),
                    procedimento.getCodDgp(),
                    procedimento.getDescricaoProcedimento(),
                    item.getQuantidadeSessoes(),
                    item.getValorSessao(),
                    valorTotal,
                    contrato,
                    item.getOrigemContrato() == null ? null : item.getOrigemContrato().name(),
                    item.getObservacao(),
                    ControleFluxoFatura.glosaDaLinha(item),
                    comparacao(item.getValorSessao(), contrato));
        }

        private static String comparacao(BigDecimal valorSessao, BigDecimal contrato) {
            if (contrato == null) {
                return "SEM_CONTRATO";
            }
            int cmp = valorSessao.compareTo(contrato);
            if (cmp == 0) {
                return "CONFORME";
            }
            return cmp > 0 ? "ACIMA" : "ABAIXO";
        }
    }

    public record TotaisResponse(
            BigDecimal informado,
            BigDecimal glosa,
            @JsonProperty("final") BigDecimal valorFinal) {
    }

    public record EventoResponse(
            String tipo,
            LocalDateTime data,
            String usuario,
            String responsavel,
            String motivo) {

        public static EventoResponse fromEntity(FaturaEvento evento) {
            return new EventoResponse(
                    evento.getTipo().name(),
                    evento.getDataEvento(),
                    evento.getUsuario().getLogin(),
                    evento.getResponsavel(),
                    null);
        }
    }

    public record EtapaResponse(
            String etapa,
            String setor,
            LocalDateTime dataEntrada,
            LocalDateTime dataSaida,
            String responsavel,
            BigDecimal valorInicial,
            BigDecimal valorFinal,
            String motivo) {

        public static EtapaResponse fromEntity(EtapaFluxo etapa) {
            return new EtapaResponse(
                    etapa.etapa(),
                    etapa.setor(),
                    etapa.dataEntrada(),
                    etapa.dataSaida(),
                    etapa.responsavel(),
                    etapa.valorInicial(),
                    etapa.valorFinal(),
                    null);
        }
    }
}