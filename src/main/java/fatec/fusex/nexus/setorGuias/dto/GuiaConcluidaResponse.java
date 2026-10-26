package fatec.fusex.nexus.setorGuias.dto;

import java.time.LocalDate;
import java.util.List;

/** Resposta do POST /setor-guias/pre-guias/{id}/guia/concluir (T4-BE6). */
public record GuiaConcluidaResponse(
        Long guiaId,
        String numero,
        LocalDate validadeAte,
        List<String> assinaturasPendentes) {
}
