package fatec.fusex.nexus.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void naoEncontrado_devolve404ComMensagem() {
        ResponseEntity<ErrorResponse> resposta = handler.naoEncontrado(new RecursoNaoEncontradoException("sumiu"));

        assertThat(resposta.getStatusCode().value()).isEqualTo(404);
        assertThat(resposta.getBody()).isNotNull();
        assertThat(resposta.getBody().mensagem()).isEqualTo("sumiu");
    }

    @Test
    void regraDeNegocio_devolve400() {
        ResponseEntity<ErrorResponse> resposta = handler.regraDeNegocio(new RegraDeNegocioException("regra violada"));

        assertThat(resposta.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void conflito_devolve409() {
        ResponseEntity<ErrorResponse> resposta = handler.conflito(new ConflitoException("CPF ja cadastrado"));

        assertThat(resposta.getStatusCode().value()).isEqualTo(409);
    }
}
