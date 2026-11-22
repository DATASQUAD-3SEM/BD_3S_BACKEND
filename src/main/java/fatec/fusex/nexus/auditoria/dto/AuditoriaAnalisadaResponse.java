package fatec.fusex.nexus.auditoria.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.setorFatura.ControleFluxoFatura;
import fatec.fusex.nexus.setorFatura.FaturaEvento;
import fatec.fusex.nexus.setorFatura.TipoFaturaEvento;

/** Fatura ja visualizada pela auditoria (T26-BE3): historico em modo consulta. */
public record AuditoriaAnalisadaResponse(
        Long id,
        String ocs,
        String numeroFatura,
        LocalDateTime dataEntrada,
        LocalDateTime dataVisualizacao,
        String responsavelVisualizacao,
        BigDecimal valorInformado,
        BigDecimal valorGlosa,
        BigDecimal valorFinal) {

    public static AuditoriaAnalisadaResponse fromEntity(Espelho espelho, List<EspelhoItem> itens,
                                                        FaturaEvento visualizacao) {
        BigDecimal informado = ControleFluxoFatura.valorInformado(itens).setScale(2);
        BigDecimal valorFinal = ControleFluxoFatura.valorFinal(itens);
        BigDecimal glosa = valorFinal == null ? null : informado.subtract(valorFinal).setScale(2);
        return new AuditoriaAnalisadaResponse(
                espelho.getIdEspelho(),
                espelho.getOcs().getNome(),
                espelho.getNumeroFatura(),
                espelho.getDataEnvio(),
                visualizacao == null ? null : visualizacao.getDataEvento(),
                visualizacao == null ? null : visualizacao.getResponsavel(),
                informado,
                glosa,
                valorFinal);
    }

    public static FaturaEvento ultimaVisualizacao(List<FaturaEvento> eventos) {
        FaturaEvento ultima = null;
        for (FaturaEvento evento : eventos) {
            if (evento.getTipo() == TipoFaturaEvento.VISUALIZADA) {
                ultima = evento;
            }
        }
        return ultima;
    }
}
