package fatec.fusex.nexus.glosaOcs.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import fatec.fusex.nexus.setorFatura.dto.FaturaDetalheResponse.EtapaResponse;

/** Detalhe da glosa para a OCS (T27-BE1), com o controle de fluxo da fatura. */
public record GlosaDetalheResponse(
        Long id,
        String numeroFatura,
        LocalDateTime dataEnvioOcs,
        LocalDateTime prazoOcs,
        boolean prazoVencido,
        LocalDateTime dataCiencia,
        String responsavelCiencia,
        BigDecimal valorInformado,
        BigDecimal valorGlosa,
        BigDecimal valorFinal,
        List<GlosaGuiaResponse> guiasGlosadas,
        List<EtapaResponse> controleFluxo) {
}
