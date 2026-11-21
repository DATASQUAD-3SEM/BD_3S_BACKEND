package fatec.fusex.nexus.glosaOcs.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.setorFatura.ControleFluxoFatura;

/** Fatura com glosa pendente de decisao da OCS (T27-BE1). Valores so das linhas com glosa. */
public record GlosaResumoResponse(
        Long id,
        String numeroFatura,
        LocalDateTime dataEnvioOcs,
        LocalDateTime prazoOcs,
        boolean prazoVencido,
        BigDecimal valorInformado,
        BigDecimal valorGlosa,
        BigDecimal valorFinal) {

    public static GlosaResumoResponse fromEntity(Espelho espelho, List<EspelhoItem> itens,
                                                 LocalDateTime dataEnvioOcs, LocalDateTime prazo,
                                                 boolean prazoVencido) {
        List<EspelhoItem> comGlosa = itens.stream()
                .filter(item -> ControleFluxoFatura.glosaDaLinha(item).signum() != 0).toList();
        BigDecimal informado = ControleFluxoFatura.valorInformado(comGlosa).setScale(2);
        BigDecimal valorFinal = ControleFluxoFatura.valorFinal(comGlosa);
        BigDecimal glosa = valorFinal == null ? null : informado.subtract(valorFinal).setScale(2);
        return new GlosaResumoResponse(espelho.getIdEspelho(), espelho.getNumeroFatura(), dataEnvioOcs, prazo,
                prazoVencido, informado, glosa, valorFinal);
    }
}
