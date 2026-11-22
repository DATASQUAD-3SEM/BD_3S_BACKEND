package fatec.fusex.nexus.auditoria.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import fatec.fusex.nexus.espelho.Espelho;
import fatec.fusex.nexus.espelho.EspelhoItem;
import fatec.fusex.nexus.setorFatura.ControleFluxoFatura;
import fatec.fusex.nexus.setorFatura.FaturaEvento;
import fatec.fusex.nexus.setorFatura.TipoFaturaEvento;

/** Fatura em auditoria (T26-BE1): lista de faturas aguardando visualizacao. */
public record AuditoriaResumoResponse(
        Long id,
        String ocs,
        String numeroFatura,
        LocalDateTime dataEntrada,
        LocalDateTime dataInicio,
        LocalDateTime dataEnvio,
        LocalDateTime dataRecebimentoAuditoria,
        String situacao,
        BigDecimal valorInformado,
        BigDecimal valorGlosa,
        BigDecimal valorFinal) {

    public static AuditoriaResumoResponse fromEntity(Espelho espelho, List<EspelhoItem> itens,
                                                     List<FaturaEvento> eventos) {
        BigDecimal informado = ControleFluxoFatura.valorInformado(itens).setScale(2);
        BigDecimal valorFinal = ControleFluxoFatura.valorFinal(itens);
        BigDecimal glosa = valorFinal == null ? null : informado.subtract(valorFinal).setScale(2);
        return new AuditoriaResumoResponse(
                espelho.getIdEspelho(),
                espelho.getOcs().getNome(),
                espelho.getNumeroFatura(),
                espelho.getDataEnvio(),
                espelho.getDataCriacao(),
                espelho.getDataEnvio(),
                ultimoEvento(eventos, TipoFaturaEvento.ENVIADA_AUDITORIA),
                espelho.getSituacaoFatura().name(),
                informado,
                glosa,
                valorFinal);
    }

    public static LocalDateTime ultimoEvento(List<FaturaEvento> eventos, TipoFaturaEvento tipo) {
        LocalDateTime data = null;
        for (FaturaEvento evento : eventos) {
            if (evento.getTipo() == tipo) {
                data = evento.getDataEvento();
            }
        }
        return data;
    }
}
