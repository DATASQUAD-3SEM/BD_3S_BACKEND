package fatec.fusex.nexus.versoGuia.dto;

import java.math.BigDecimal;

/** Fatura com Guias glosadas com verso disponivel (T32-BE1, lista da T32-FE1). */
public record VersoFaturaResponse(
        Long faturaId,
        String numeroFatura,
        String ocs,
        String situacao,
        int guiasGlosadas,
        BigDecimal valorGlosa) {
}
