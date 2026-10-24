package fatec.fusex.nexus.ocs.dto;

import java.util.List;


public record OcsBuscaResponse(
        List<OcsResponse> resultados,
        long total
) {
}