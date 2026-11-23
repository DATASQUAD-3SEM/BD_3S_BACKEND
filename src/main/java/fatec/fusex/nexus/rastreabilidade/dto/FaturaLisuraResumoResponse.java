package fatec.fusex.nexus.rastreabilidade.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.espelho.SituacaoFatura;
import fatec.fusex.nexus.setorFatura.ControleFluxoFatura;
import fatec.fusex.nexus.setorFatura.FaturaEvento;
import fatec.fusex.nexus.setorFatura.FaturaTransicaoService;
import fatec.fusex.nexus.setorFatura.TipoFaturaEvento;

public record FaturaLisuraResumoResponse(
        Long id,
        String ocs,
        LocalDateTime dataInicio,
        LocalDateTime dataEnvio,
        LocalDateTime dataEntrada,
        String numeroFatura,
        String situacao,
        BigDecimal valorInformado,
        long linhasDecididas,
        int totalLinhas,
        LocalDateTime dataRecebimento,
        LocalDateTime dataEnvioOcs,
        LocalDateTime prazoOcs,
        boolean liberadaParaAuditoria) {

    public static FaturaLisuraResumoResponse fromEntity(Espelho espelho, List<EspelhoItem> itens,
                                                        List<FaturaEvento> eventos, LocalDateTime agora) {
        LocalDateTime dataEnvioOcs = eventos.stream()
                .filter(e -> e.getTipo() == TipoFaturaEvento.ENVIADA_OCS)
                .map(FaturaEvento::getDataEvento)
                .reduce((antes, depois) -> depois)
                .orElse(null);

        return new FaturaLisuraResumoResponse(
                espelho.getIdEspelho(),
                espelho.getOcs().getNome(),
                espelho.getDataCriacao(),
                espelho.getDataEnvio(),
                espelho.getDataEnvio(),
                espelho.getNumeroFatura(),
                espelho.getSituacaoFatura().name(),
                ControleFluxoFatura.valorInformado(itens).setScale(2),
                itens.stream().filter(item -> item.getValorContratoSessao() != null).count(),
                itens.size(),
                eventos.stream()
                        .filter(e -> e.getTipo() == TipoFaturaEvento.ENVIADA_LISURA)
                        .map(FaturaEvento::getDataEvento)
                        .findFirst()
                        .orElse(null),
                dataEnvioOcs,
                dataEnvioOcs == null ? null
                        : FaturaTransicaoService.somarDiasUteis(dataEnvioOcs,
                        FaturaTransicaoService.PRAZO_OCS_DIAS_UTEIS),
                liberada(espelho.getSituacaoFatura(), itens, eventos, agora));
    }

    public static boolean liberada(SituacaoFatura situacao, List<EspelhoItem> itens,
                                    List<FaturaEvento> eventos, LocalDateTime agora) {
        if (situacao == SituacaoFatura.EM_LISURA) {
            return itens.stream().allMatch(item -> item.getValorContratoSessao() != null)
                    && itens.stream().allMatch(item -> ControleFluxoFatura.glosaDaLinha(item).signum() == 0);
        }
        if (situacao == SituacaoFatura.AGUARDANDO_OCS) {
            return FaturaTransicaoService.prazoLiberado(eventos, agora);
        }
        return false;
    }
}