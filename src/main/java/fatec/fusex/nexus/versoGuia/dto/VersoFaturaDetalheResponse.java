package fatec.fusex.nexus.versoGuia.dto;

import java.math.BigDecimal;
import java.util.List;

/** Guias glosadas de uma fatura com verso disponivel (T32-BE1, tela da T32-FE2), na ordem do numero da Guia. */
public record VersoFaturaDetalheResponse(
        Long faturaId,
        String numeroFatura,
        String ocs,
        List<VersoGuiaResponse> guias) {

    public record VersoGuiaResponse(
            Long guiaId,
            String guiaNumero,
            String paciente,
            String situacaoFinal,
            String seloFinal,
            BigDecimal glosa) {
    }
}
