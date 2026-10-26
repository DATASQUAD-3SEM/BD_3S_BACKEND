package fatec.fusex.nexus.chefeFusex;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.validation.ValidacaoCadastro;

/** Regras de formato do cadastro de medico (CPF e CRM), sem banco. */
class ChefeMedicoCadastroServiceTest {

    @Test
    void cpf_validoComOuSemMascara_devolveSoDigitos() {
        assertThat(ValidacaoCadastro.normalizarCpf("111.444.777-35")).isEqualTo("11144477735");
        assertThat(ValidacaoCadastro.normalizarCpf("52998224725")).isEqualTo("52998224725");
        assertThat(ValidacaoCadastro.normalizarCpf(" 390.533.447-05 ")).isEqualTo("39053344705");
    }

    @Test
    void cpf_digitoVerificadorErrado_lanca422() {
        assertCpfInvalido("123.456.789-00");
        assertCpfInvalido("11144477736");
    }

    @Test
    void cpf_formatoOuSequenciaInvalidos_lanca422() {
        assertCpfInvalido("11111111111"); // todos iguais passam no calculo, mas nao sao CPF
        assertCpfInvalido("1234567890");  // 10 digitos
        assertCpfInvalido("abc.def.ghi-jk");
        assertCpfInvalido(null);
        assertCpfInvalido("  ");
    }

    @Test
    void crm_aceitaSeparadoresEMinusculas_eNormalizaParaNumeroHifenUf() {
        assertThat(ChefeMedicoCadastroService.normalizarCrm("123456-SP")).isEqualTo("123456-SP");
        assertThat(ChefeMedicoCadastroService.normalizarCrm("123456/sp")).isEqualTo("123456-SP");
        assertThat(ChefeMedicoCadastroService.normalizarCrm("1234 rj")).isEqualTo("1234-RJ");
        assertThat(ChefeMedicoCadastroService.normalizarCrm("1234567DF")).isEqualTo("1234567-DF");
    }

    @Test
    void crm_formatoInvalido_lanca422NoCampoCrm() {
        for (String crm : new String[] {"ABC", "123-SP", "12345678-SP", "123456-XX", "123456", "", null}) {
            assertThatThrownBy(() -> ChefeMedicoCadastroService.normalizarCrm(crm))
                    .as("crm '%s'", crm)
                    .isInstanceOf(DadosInvalidosException.class)
                    .extracting(e -> ((DadosInvalidosException) e).getCampo())
                    .isEqualTo("crm");
        }
    }

    private static void assertCpfInvalido(String cpf) {
        assertThatThrownBy(() -> ValidacaoCadastro.normalizarCpf(cpf))
                .as("cpf '%s'", cpf)
                .isInstanceOf(DadosInvalidosException.class)
                .extracting(e -> ((DadosInvalidosException) e).getCampo())
                .isEqualTo("cpf");
    }
}
