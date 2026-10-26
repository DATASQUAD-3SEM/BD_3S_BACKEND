package fatec.fusex.nexus.chefeBeneficiario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.common.validation.ValidacaoCadastro;

/** Regras de formato do cadastro de beneficiario (CPF, data de nascimento e telefone), sem banco. */
class ChefeBeneficiarioCadastroServiceTest {

    @Test
    void cpf_validoComOuSemMascara_devolveSoDigitos() {
        assertThat(ValidacaoCadastro.normalizarCpf("529.982.247-25")).isEqualTo("52998224725");
        assertThat(ValidacaoCadastro.normalizarCpf("11144477735")).isEqualTo("11144477735");
    }

    @Test
    void cpf_invalido_lanca422NoCampoCpf() {
        for (String cpf : new String[] {"123.456.789-00", "11111111111", "1234567890", "abc", "", null}) {
            assertCampoInvalido(() -> ValidacaoCadastro.normalizarCpf(cpf), "cpf", cpf);
        }
    }

    @Test
    void dataNascimento_noPassado_eAceita() {
        assertThat(ChefeBeneficiarioCadastroService.validarDataNascimento("1968-03-15"))
                .isEqualTo(LocalDate.of(1968, 3, 15));
        assertThat(ChefeBeneficiarioCadastroService.validarDataNascimento(Beneficiario.hoje().minusDays(1).toString()))
                .isEqualTo(Beneficiario.hoje().minusDays(1));
    }

    @Test
    void dataNascimento_hojeFuturaOuMalFormatada_lanca422() {
        String[] invalidas = {
                Beneficiario.hoje().toString(),
                Beneficiario.hoje().plusYears(1).toString(),
                "15/03/1968", "1968-02-30", "", null};
        for (String data : invalidas) {
            assertCampoInvalido(() -> ChefeBeneficiarioCadastroService.validarDataNascimento(data), "dataNascimento", data);
        }
    }

    @Test
    void telefone_celularComDddComOuSemMascara_devolveSoDigitos() {
        assertThat(ChefeBeneficiarioCadastroService.normalizarTelefone("12987654321")).isEqualTo("12987654321");
        assertThat(ChefeBeneficiarioCadastroService.normalizarTelefone("(12) 98765-4321")).isEqualTo("12987654321");
        assertThat(ChefeBeneficiarioCadastroService.normalizarTelefone("12 98765-4321")).isEqualTo("12987654321");
    }

    @Test
    void telefone_invalido_lanca422NoCampoTelefone() {
        for (String telefone : new String[] {"1234", "1287654321", "02987654321", "abc", "", null}) {
            assertCampoInvalido(() -> ChefeBeneficiarioCadastroService.normalizarTelefone(telefone), "telefone", telefone);
        }
    }

    private static void assertCampoInvalido(Runnable chamada, String campo, String valor) {
        assertThatThrownBy(chamada::run)
                .as("%s '%s'", campo, valor)
                .isInstanceOf(DadosInvalidosException.class)
                .extracting(e -> ((DadosInvalidosException) e).getCampo())
                .isEqualTo(campo);
    }
}
