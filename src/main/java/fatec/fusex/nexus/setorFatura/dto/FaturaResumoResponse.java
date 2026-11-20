package fatec.fusex.nexus.setorFatura.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.setorFatura.ControleFluxoFatura;

public record FaturaResumoResponse(
        Long id,
        String ocs,
        LocalDateTime dataInicio,
        LocalDateTime dataEnvio,
        LocalDateTime dataEntrada,
        String situacao,
        String numeroFatura,
        BigDecimal valorTotal) {

    public static FaturaResumoResponse fromEntity(Espelho espelho, List<EspelhoItem> itens) {
        return new FaturaResumoResponse(
                espelho.getIdEspelho(),
                espelho.getOcs().getNome(),
                espelho.getDataCriacao(),
                espelho.getDataEnvio(),
                espelho.getDataEnvio(),
                espelho.getSituacaoFatura().name(),
                espelho.getNumeroFatura(),
                ControleFluxoFatura.valorInformado(itens).setScale(2));
    }
}