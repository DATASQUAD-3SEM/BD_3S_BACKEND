package fatec.fusex.nexus.common.exception;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Transforma excecoes em respostas JSON padronizadas.
 * Por isso NAO precisa de try/catch no controller: basta lancar a excecao certa.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> naoEncontrado(RecursoNaoEncontradoException ex) {
        return montar(404, "Not Found", ex.getMessage(), List.of());
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErrorResponse> regraDeNegocio(RegraDeNegocioException ex) {
        return montar(400, "Bad Request", ex.getMessage(), List.of());
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErrorResponse> conflito(ConflitoException ex) {
        return montar(409, "Conflict", ex.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> validacao(MethodArgumentNotValidException ex) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> erro.getField() + ": " + erro.getDefaultMessage())
                .toList();
        return montar(400, "Bad Request", "Dados invalidos", detalhes);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> arquivoGrande(MaxUploadSizeExceededException ex) {
        return montar(413, "Payload Too Large", "Arquivo maior que o limite permitido (10MB)", List.of());
    }

    private ResponseEntity<ErrorResponse> montar(int status, String erro, String mensagem, List<String> detalhes) {
        ErrorResponse corpo = new ErrorResponse(status, erro, mensagem, detalhes, LocalDateTime.now());
        return ResponseEntity.status(status).body(corpo);
    }
}
