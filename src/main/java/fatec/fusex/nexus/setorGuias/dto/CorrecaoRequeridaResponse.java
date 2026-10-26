package fatec.fusex.nexus.setorGuias.dto;

import java.time.LocalDateTime;

/** Resposta do POST /setor-guias/pre-guias/{id}/correcao (T4-BE4). */
public record CorrecaoRequeridaResponse(String status, LocalDateTime dataDecisao) {
}
