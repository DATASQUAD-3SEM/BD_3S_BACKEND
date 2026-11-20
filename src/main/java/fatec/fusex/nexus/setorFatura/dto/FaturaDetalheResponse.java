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
                new TotaisResponse(informado, null, null),
                eventos.stream().map(EventoResponse::fromEntity).toList(),
                etapas.stream().map(EtapaResponse::fromEntity).toList());
    }

    public record LinhaResponse(
            String guiaNumero,
            String paciente,
            String codigo,
            String procedimento,
            Integer quantidadeSessoes,
            BigDecimal valorSessao,
            BigDecimal valorTotal) {

        public static LinhaResponse fromEntity(EspelhoItem item) {
            GuiaProcedimento procedimento = item.getGuiaProcedimento();
            Guia guia = procedimento.getGuia();
            BigDecimal valorTotal = item.getValorSessao()
                    .multiply(BigDecimal.valueOf(item.getQuantidadeSessoes()))
                    .setScale(2);
            return new LinhaResponse(
                    guia.getNumero(),
                    guia.getPreGuia().getBeneficiario().getNome(),
                    procedimento.getCodDgp(),
                    procedimento.getDescricaoProcedimento(),
                    item.getQuantidadeSessoes(),
                    item.getValorSessao(),
                    valorTotal);
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