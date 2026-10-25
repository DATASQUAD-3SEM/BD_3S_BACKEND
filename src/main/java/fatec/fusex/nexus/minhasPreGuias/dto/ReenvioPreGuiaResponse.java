package fatec.fusex.nexus.minhasPreGuias.dto;

import java.time.LocalDateTime;

/** Resposta do POST /beneficiarios/me/pre-guias/{id}/reenvio (T9-BE4). */
public record ReenvioPreGuiaResponse(String status, LocalDateTime dataReenvio) {
}
