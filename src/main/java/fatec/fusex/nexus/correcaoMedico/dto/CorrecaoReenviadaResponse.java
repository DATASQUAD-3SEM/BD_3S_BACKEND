package fatec.fusex.nexus.correcaoMedico.dto;

import java.time.LocalDateTime;

/** Resposta do PUT /medicos/me/pre-guias/{id}/correcao (T14-BE5). */
public record CorrecaoReenviadaResponse(
        String status,
        LocalDateTime dataReenvio) {
}
