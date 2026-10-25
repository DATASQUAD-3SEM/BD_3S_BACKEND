package fatec.fusex.nexus.ocs.dto;

import java.util.List;


/**
 * Resposta das buscas de OCS do medico: ate 30 resultados e o total encontrado antes do corte.
 */
public record OcsBuscaResponse(
        List<OcsResponse> resultados,
        long total
) {
}