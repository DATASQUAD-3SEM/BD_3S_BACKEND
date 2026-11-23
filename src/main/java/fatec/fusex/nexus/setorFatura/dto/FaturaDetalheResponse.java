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
import fatec.fusex.nexus.rastreabilidade.dto.FaturaLisuraResumoResponse;
import fatec.fusex.nexus.setorFatura.FaturaEvento;
import fatec.fusex.nexus.setorFatura.FaturaTransicaoService;
import fatec.fusex.nexus.setorFatura.TipoFaturaEvento;

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
        List<EtapaResponse> controleFluxo,
        LocalDateTime dataEnvioOcs,
        LocalDateTime prazoOcs,
        LocalDateTime dataCiencia,
        String responsavelCiencia,
        boolean liberadaParaAuditoria) {

    public static FaturaDetalheResponse fromEntity(Espelho espelho, List<EspelhoItem> itens,
                                                   List<FaturaEvento> eventos, List<EtapaFluxo> etapas,
                                                   LocalDateTime agora) {
        BigDecimal informado = ControleFluxoFatura.valorInformado(itens).setScale(2);
        BigDecimal valorFinal = ControleFluxoFatura.valorFinal(itens);
        BigDecimal glosa = valorFinal == null ? null : informado.subtract(valorFinal).setScale(2);
        // Envio mais recente à OCS e a ciência que veio depois dele (o que liberou a fatura).
        FaturaEvento envio = null;
        FaturaEvento ciencia = null;
        for (FaturaEvento evento : eventos) {
            if (evento.getTipo() == TipoFaturaEvento.ENVIADA_OCS) {
                envio = evento;
                ciencia = null;
            } else if (envio != null && evento.getTipo() == TipoFaturaEvento.CIENTE_OCS) {
                ciencia = evento;
            }
        }
        LocalDateTime envioOcs = envio == null ? null : envio.getDataEvento();
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
                etapas.stream().map(EtapaResponse::fromEntity).toList(),
                envioOcs,
                envioOcs == null ? null : FaturaTransicaoService.somarDiasUteis(envioOcs, FaturaTransicaoService.PRAZO_OCS_DIAS_UTEIS),
                ciencia == null ? null : ciencia.getDataEvento(),
                ciencia == null ? null : ciencia.getResponsavel(),
                FaturaLisuraResumoResponse.liberada(espelho.getSituacaoFatura(), itens, eventos, agora));
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
            String comparacao,
            String decisaoOcs,
            String motivoContestacaoOcs) {

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
                    comparacao(item.getValorSessao(), contrato),
                    item.getDecisaoOcs(),
                    item.getMotivoContestacaoOcs());
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