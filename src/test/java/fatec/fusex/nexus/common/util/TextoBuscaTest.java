package fatec.fusex.nexus.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.common.exception.RegraDeNegocioException;

/** Auxiliares dos parametros de busca das listagens. */
class TextoBuscaTest {

    @Test
    void escaparCuringas_tratamPorcentoESublinhadoComoTexto() {
        assertThat(TextoBusca.escaparCuringas("50%_a!")).isEqualTo("50!%!_a!!");
    }

    @Test
    void digitosDaBusca_comMascara_devolveSoDigitos() {
        assertThat(TextoBusca.digitosDaBusca("529.982.247-25")).isEqualTo("52998224725");
    }

    @Test
    void digitosDaBusca_comLetras_devolveVazio() {
        assertThat(TextoBusca.digitosDaBusca("Antonio 123")).isEmpty();
        assertThat(TextoBusca.digitosDaBusca("")).isEmpty();
    }

    @Test
    void filtroDeSituacao_converteOsTresValores() {
        assertThat(TextoBusca.filtroDeSituacao(null)).isTrue();
        assertThat(TextoBusca.filtroDeSituacao("Ativos")).isTrue();
        assertThat(TextoBusca.filtroDeSituacao("inativos")).isFalse();
        assertThat(TextoBusca.filtroDeSituacao("todos")).isNull();
    }

    @Test
    void filtroDeSituacao_valorInvalido_lanca400() {
        assertThatThrownBy(() -> TextoBusca.filtroDeSituacao("excluidos"))
                .isInstanceOf(RegraDeNegocioException.class);
    }
}
