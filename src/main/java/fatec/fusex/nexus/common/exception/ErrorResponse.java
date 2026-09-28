package fatec.fusex.nexus.common.exception;

import java.time.LocalDateTime;
import java.util.List;

/** Formato UNICO de erro da API. O front (React) le sempre estes campos. */
public record ErrorResponse(
        int status,
        String erro,
        String mensagem,
        List<String> detalhes,
        LocalDateTime timestamp) {
}
