package fatec.fusex.nexus.setorGuias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.common.exception.LimiteRequisicoesException;

/** Regras puras do T11-BE1: extracao do numero no texto do PDF e limite de leituras por minuto. */
class ExtrairNumeroGuiaServiceTest {

    @Test
    void extrai_o_numero_depois_do_cabecalho() {
        assertThat(ExtrairNumeroGuiaService.extrairNumeroDoTexto("GUIA DE ENCAMINHAMENTO\nNr: 202610678"))
                .contains("202610678");
    }

    @Test
    void aceita_espacos_e_caixa_diferente_no_cabecalho() {
        assertThat(ExtrairNumeroGuiaService.extrairNumeroDoTexto("Guia de  Encaminhamento\nNr:202610678"))
                .contains("202610678");
    }

    @Test
    void sem_o_cabecalho_nao_extrai() {
        assertThat(ExtrairNumeroGuiaService.extrairNumeroDoTexto("Nr: 202610678")).isEmpty();
    }

    @Test
    void cabecalho_sem_o_padrao_nao_extrai() {
        assertThat(ExtrairNumeroGuiaService.extrairNumeroDoTexto("GUIA DE ENCAMINHAMENTO\nSem numero")).isEmpty();
    }

    @Test
    void numero_com_mais_de_20_digitos_nao_extrai() {
        assertThat(ExtrairNumeroGuiaService.extrairNumeroDoTexto(
                "GUIA DE ENCAMINHAMENTO Nr: 123456789012345678901")).isEmpty();
    }

    @Test
    void texto_vazio_ou_nulo_nao_extrai() {
        assertThat(ExtrairNumeroGuiaService.extrairNumeroDoTexto(null)).isEmpty();
        assertThat(ExtrairNumeroGuiaService.extrairNumeroDoTexto("   ")).isEmpty();
    }

    @Test
    void limite_de_leituras_bloqueia_a_21a_e_libera_na_janela_seguinte() {
        RelogioManual relogio = new RelogioManual(Instant.parse("2026-11-05T12:00:00Z"));
        LimiteLeituraGuia limite = new LimiteLeituraGuia(relogio);

        for (int i = 0; i < 20; i++) {
            limite.registrar("setor.teste");
        }
        assertThatThrownBy(() -> limite.registrar("setor.teste"))
                .isInstanceOf(LimiteRequisicoesException.class);

        relogio.avancar(Duration.ofMinutes(1));
        limite.registrar("setor.teste");
    }

    /** Relogio que o teste avanca na mao, para cobrir a janela de um minuto sem esperar. */
    private static final class RelogioManual extends Clock {

        private Instant agora;

        RelogioManual(Instant inicio) {
            this.agora = inicio;
        }

        void avancar(Duration duracao) {
            agora = agora.plus(duracao);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }
}
