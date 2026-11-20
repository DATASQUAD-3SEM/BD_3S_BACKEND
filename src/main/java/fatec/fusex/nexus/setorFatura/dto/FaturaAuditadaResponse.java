package fatec.fusex.nexus.setorFatura.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.setorFatura.ControleFluxoFatura;

public record FaturaAuditadaResponse(
        Long id,
        String ocs,
        LocalDateTime dataInicio,
        LocalDateTime dataEnvio,
        String numeroFatura,
        LocalDateTime dataEntrada,
        LocalDateTime dataVisualizacao,
        BigDecimal valorInformado,
        BigDecimal valorGlosa,
        BigDecimal valorFinal) {

    public static FaturaAuditadaResponse fromEntity(Espelho espelho, List<EspelhoItem> itens,
                                                    LocalDateTime dataVisualizacao) {
        BigDecimal informado = ControleFluxoFatura.valorInformado(itens).setScale(2);
        BigDecimal valorFinal = ControleFluxoFatura.valorFinal(itens);
        BigDecimal glosa = valorFinal == null ? null : informado.subtract(valorFinal).setScale(2);
        return new FaturaAuditadaResponse(
                espelho.getIdEspelho(),
                espelho.getOcs().getNome(),
                espelho.getDataCriacao(),
                espelho.getDataEnvio(),
                espelho.getNumeroFatura(),
                espelho.getDataEnvio(),
                dataVisualizacao,
                informado,
                glosa,
                valorFinal);
    }
}