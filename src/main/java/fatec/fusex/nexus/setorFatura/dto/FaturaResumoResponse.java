package fatec.fusex.nexus.setorFatura.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.setorFatura.ControleFluxoFatura;
import fatec.fusex.nexus.setorFatura.FaturaEvento;
import fatec.fusex.nexus.setorFatura.FaturaTransicaoService;
import fatec.fusex.nexus.setorFatura.TipoFaturaEvento;

public record FaturaResumoResponse(
        Long id,
        String ocs,
        LocalDateTime dataInicio,
        LocalDateTime dataEnvio,
        LocalDateTime dataEntrada,
        String situacao,
        String numeroFatura,
        BigDecimal valorTotal,
        // T24-BE2 (regra 12.8): prazo da OCS para o aviso do setor de Faturas. Nulos quando nao houve envio a OCS.
        LocalDateTime dataEnvioOcs,
        LocalDateTime prazoOcs,
        LocalDateTime dataCiencia,
        // T24-BE2: quem deu a ciencia, para o aviso "A OCS deu ciencia das glosas em dd/mm/aaaa ([responsavel])." do cartao
        String responsavelCiencia) {

    /** Eventos da fatura em ordem de data: o envio a OCS mais recente e a ciencia que veio depois dele. */
    public static FaturaResumoResponse fromEntity(Espelho espelho, List<EspelhoItem> itens, List<FaturaEvento> eventos) {
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
        return new FaturaResumoResponse(
                espelho.getIdEspelho(),
                espelho.getOcs().getNome(),
                espelho.getDataCriacao(),
                espelho.getDataEnvio(),
                espelho.getDataEnvio(),
                espelho.getSituacaoFatura().name(),
                espelho.getNumeroFatura(),
                ControleFluxoFatura.valorInformado(itens).setScale(2),
                envioOcs,
                envioOcs == null ? null
                        : FaturaTransicaoService.somarDiasUteis(envioOcs, FaturaTransicaoService.PRAZO_OCS_DIAS_UTEIS),
                ciencia == null ? null : ciencia.getDataEvento(),
                ciencia == null ? null : ciencia.getResponsavel());
    }
}