package fatec.fusex.nexus.guiaAssinatura;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.common.exception.LimiteRequisicoesException;

class LimiteLoteAssinaturaTest {

    @Test
    void ateDezLotesPorMinuto_passam_oDecimoPrimeiro_recusa() {
        Relogio relogio = new Relogio(Instant.parse("2026-11-08T10:00:00Z"));
        LimiteLoteAssinatura limite = new LimiteLoteAssinatura(relogio);

        for (int i = 0; i < 10; i++) {
            limite.registrar("chefe");
        }
        assertThatThrownBy(() -> limite.registrar("chefe"))
                .isInstanceOf(LimiteRequisicoesException.class);
    }

    @Test
    void novaJanelaLiberaDeNovo() {
        Relogio relogio = new Relogio(Instant.parse("2026-11-08T10:00:00Z"));
        LimiteLoteAssinatura limite = new LimiteLoteAssinatura(relogio);
        for (int i = 0; i < 10; i++) {
            limite.registrar("chefe");
        }

        relogio.avancar(Duration.ofMinutes(1));

        assertThatCode(() -> limite.registrar("chefe")).doesNotThrowAnyException();
    }

    @Test
    void contagemEEspecificaPorLogin() {
        Relogio relogio = new Relogio(Instant.parse("2026-11-08T10:00:00Z"));
        LimiteLoteAssinatura limite = new LimiteLoteAssinatura(relogio);
        for (int i = 0; i < 10; i++) {
            limite.registrar("chefe");
        }

        assertThatCode(() -> limite.registrar("outro")).doesNotThrowAnyException();
    }

    // Relogio manual para controlar a janela de tempo nos testes
    private static final class Relogio extends Clock {
        private Instant agora;

        Relogio(Instant inicio) {
            this.agora = inicio;
        }

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
