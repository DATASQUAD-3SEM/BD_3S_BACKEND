package fatec.fusex.nexus.beneficiario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import fatec.fusex.nexus.common.exception.RegraDeNegocioException;

class PrecCpTest {

    @Test
    void normalizar_semMascara_devolveOMesmoValor() {
        assertThat(PrecCp.normalizar("45872213300")).isEqualTo("45872213300");
    }

    @Test
    void normalizar_comPontosEHifen_removeAMascara() {
        assertThat(PrecCp.normalizar("458.722.133-00")).isEqualTo("45872213300");
    }

    @Test
    void normalizar_somenteComHifen_removeOHifen() {
        assertThat(PrecCp.normalizar("45872213-300")).isEqualTo("45872213300");
    }

    @Test
    void normalizar_comEspacosNasPontas_ignoraOsEspacos() {
        assertThat(PrecCp.normalizar("  458.722.133-00 ")).isEqualTo("45872213300");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "-", "..."})
    void normalizar_quandoVazio_lancaExcecao(String entrada) {
        assertThatThrownBy(() -> PrecCp.normalizar(entrada))
                .isInstanceOf(RegraDeNegocioException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "1234567890",        // 10 digitos
            "123456789012",      // 12 digitos
            "4587221330A",       // letra
            "458 722 133 00",    // espaco no meio
            "458/722/133-00",    // outro separador
            "\u0664\u0665\u0668\u0667\u0662\u0662\u0661\u0663\u0663\u0660\u0660"  // 11 digitos arabes
    })
    void normalizar_quandoFormatoInvalido_lancaExcecaoSemRepetirOValor(String entrada) {
        assertThatThrownBy(() -> PrecCp.normalizar(entrada))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageNotContaining(entrada.trim());
    }
}
