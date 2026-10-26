package fatec.fusex.nexus.beneficiario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

/**
 * Testes da conversao de idade em faixa etaria.
 */
class FaixaEtariaTest {

    @ParameterizedTest
    @CsvSource({
            "0,Menor de 18", "17,Menor de 18",
            "18,18 a 30", "30,18 a 30",
            "31,31 a 40", "40,31 a 40",
            "41,41 a 50", "50,41 a 50",
            "51,51 a 60", "60,51 a 60",
            "61,61 a 70", "70,61 a 70",
            "71,Acima de 70", "120,Acima de 70"
    })
    void de_nosLimitesDasFaixas_devolveORotuloCerto(int idade, String rotulo) {
        assertThat(FaixaEtaria.de(idade).getRotulo()).isEqualTo(rotulo);
    }

    @Test
    void de_quandoIdadeNegativa_lancaExcecao() {
        assertThatThrownBy(() -> FaixaEtaria.de(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
