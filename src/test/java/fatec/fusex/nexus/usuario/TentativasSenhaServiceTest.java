package fatec.fusex.nexus.usuario;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.common.exception.MuitasTentativasException;

class TentativasSenhaServiceTest {

    @Test
    void verificarBloqueio_depoisDeCincoFalhas_bloqueiaPorQuinzeMinutos() {
        RelogioControlavel relogio =
                new RelogioControlavel(Instant.parse("2026-10-23T12:00:00Z"));

        TentativasSenhaService service =
                new TentativasSenhaService(relogio);

        for (int i = 0; i < 5; i++) {
            service.registrarFalha("usuario-teste");
        }

        assertThatThrownBy(() ->
                service.verificarBloqueio("usuario-teste"))
                .isInstanceOf(MuitasTentativasException.class);

        relogio.avancar(Duration.ofMinutes(14));

        assertThatThrownBy(() ->
                service.verificarBloqueio("usuario-teste"))
                .isInstanceOf(MuitasTentativasException.class);

        relogio.avancar(Duration.ofMinutes(1));

        assertThatCode(() ->
                service.verificarBloqueio("usuario-teste"))
                .doesNotThrowAnyException();
    }

    @Test
    void registrarSucesso_limpaFalhasAnteriores() {
        TentativasSenhaService service = new TentativasSenhaService();

        for (int i = 0; i < 4; i++) {
            service.registrarFalha("usuario-teste");
        }

        service.registrarSucesso("usuario-teste");
        service.registrarFalha("usuario-teste");

        assertThatCode(() ->
                service.verificarBloqueio("usuario-teste"))
                .doesNotThrowAnyException();
    }

    private static class RelogioControlavel extends Clock {

        private Instant instante;

        private RelogioControlavel(Instant instante) {
            this.instante = instante;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }

        private void avancar(Duration duracao) {
            instante = instante.plus(duracao);
        }
    }
}