package fatec.fusex.nexus.beneficiario;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.medicoFusex.dto.BeneficiarioMedicoResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * Testes da resposta da busca de beneficiario pelo medico: calculo de idade e faixa e omissao de CPF e telefone no toString.
 */
class BeneficiarioMedicoResponseTest {

    private static Beneficiario beneficiarioNascidoEm(LocalDate nascimento) {
        Usuario usuario = new Usuario();
        usuario.setLogin("52998224725");
        usuario.setPerfil(PerfilUsuario.BENEFICIARIO);

        Beneficiario b = new Beneficiario();
        b.setUsuarioId(7L);
        b.setUsuario(usuario);
        b.setNome("Antonio Carlos Ferreira");
        b.setPrecCp("45872213300");
        b.setDataNascimento(nascimento);
        b.setTelefone("12987654321");
        return b;
    }

    @Test
    void de_mapeiaTodosOsCampos() {
        Beneficiario b = beneficiarioNascidoEm(LocalDate.of(1968, 3, 15));

        BeneficiarioMedicoResponse r = BeneficiarioMedicoResponse.de(b, LocalDate.of(2026, 10, 21));

        assertThat(r.id()).isEqualTo(7L);
        assertThat(r.nome()).isEqualTo("Antonio Carlos Ferreira");
        assertThat(r.grupo()).isEqualTo(BeneficiarioMedicoResponse.GRUPO);
        assertThat(r.idade()).isEqualTo(58);
        assertThat(r.faixaEtaria()).isEqualTo("51 a 60");
        assertThat(r.cpf()).isEqualTo("52998224725");
        assertThat(r.telefone()).isEqualTo("12987654321");
    }

    @Test
    void de_noDiaDoAniversario_jaContaOAno() {
        Beneficiario b = beneficiarioNascidoEm(LocalDate.of(1967, 10, 21));

        BeneficiarioMedicoResponse r = BeneficiarioMedicoResponse.de(b, LocalDate.of(2026, 10, 21));

        assertThat(r.idade()).isEqualTo(59);
        assertThat(r.faixaEtaria()).isEqualTo("51 a 60");
    }

    @Test
    void de_umDiaAntesDoAniversario_aindaNaoContaOAno() {
        Beneficiario b = beneficiarioNascidoEm(LocalDate.of(1967, 10, 22));

        BeneficiarioMedicoResponse r = BeneficiarioMedicoResponse.de(b, LocalDate.of(2026, 10, 21));

        assertThat(r.idade()).isEqualTo(58);
        assertThat(r.faixaEtaria()).isEqualTo("51 a 60");
    }

    @Test
    void de_nascidoEm29DeFevereiro_calculaIdadeEmAnoNaoBissexto() {
        Beneficiario b = beneficiarioNascidoEm(LocalDate.of(2000, 2, 29));

        assertThat(BeneficiarioMedicoResponse.de(b, LocalDate.of(2026, 2, 28)).idade()).isEqualTo(25);
        assertThat(BeneficiarioMedicoResponse.de(b, LocalDate.of(2026, 3, 1)).idade()).isEqualTo(26);
    }

    @Test
    void toString_naoExpoeCpfNemTelefone() {
        Beneficiario b = beneficiarioNascidoEm(LocalDate.of(1968, 3, 15));

        String texto = BeneficiarioMedicoResponse.de(b, LocalDate.of(2026, 10, 21)).toString();

        assertThat(texto).doesNotContain("52998224725").doesNotContain("12987654321");
    }
}
