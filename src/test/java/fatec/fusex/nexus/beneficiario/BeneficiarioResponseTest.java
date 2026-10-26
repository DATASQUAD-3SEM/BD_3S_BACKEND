package fatec.fusex.nexus.beneficiario;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.beneficiario.dto.BeneficiarioResponse;
import fatec.fusex.nexus.medicoFusex.dto.BeneficiarioMedicoResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/**
 * Testes da resposta de GET /beneficiarios/me: idade, faixa etaria e omissao de dados sensiveis no toString.
 */
class BeneficiarioResponseTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 23);

    private Beneficiario beneficiario(LocalDate nascimento) {
        Usuario usuario = new Usuario();
        usuario.setLogin("52998224725");
        usuario.setPerfil(PerfilUsuario.BENEFICIARIO);

        Beneficiario b = new Beneficiario();
        b.setUsuario(usuario);
        b.setNome("Antonio Carlos Ferreira");
        b.setPrecCp("45872213300");
        b.setDataNascimento(nascimento);
        b.setTelefone("12987654321");
        return b;
    }

    @Test
    void de_grupoESempreOTextoFixoDoFusex_eNaoOPerfil() {
        BeneficiarioResponse resposta = BeneficiarioResponse.de(beneficiario(LocalDate.of(1978, 5, 10)));

        assertThat(resposta.grupo()).isEqualTo(BeneficiarioMedicoResponse.GRUPO);
        assertThat(resposta.grupo()).isEqualTo("Ex\u00e9rcito Brasileiro \u00b7 FUSEX");
    }

    @Test
    void de_calculaIdadeEFaixaAPartirDaDataDeNascimento() {
        BeneficiarioResponse resposta = BeneficiarioResponse.de(
                beneficiario(LocalDate.of(1968, 5, 10)), HOJE);

        assertThat(resposta.idade()).isEqualTo(58);
        assertThat(resposta.faixaEtaria()).isEqualTo("51 a 60");
    }

    @Test
    void de_antesDoAniversario_idadeEUmAnoAMenos() {
        BeneficiarioResponse resposta = BeneficiarioResponse.de(
                beneficiario(LocalDate.of(1968, 12, 1)), HOJE);

        assertThat(resposta.idade()).isEqualTo(57);
    }

    @Test
    void de_assinaturaCadastrada_refleteSeExisteAssinaturaUrl() {
        Beneficiario comAssinatura = beneficiario(LocalDate.of(1968, 5, 10));
        comAssinatura.getUsuario().setAssinaturaUrl("uploads/assinatura.png");

        Beneficiario semAssinatura = beneficiario(LocalDate.of(1968, 5, 10));

        assertThat(BeneficiarioResponse.de(comAssinatura, HOJE).assinaturaCadastrada()).isTrue();
        assertThat(BeneficiarioResponse.de(semAssinatura, HOJE).assinaturaCadastrada()).isFalse();
    }

    @Test
    void toString_naoExpoeCpfNemTelefone() {
        BeneficiarioResponse resposta = BeneficiarioResponse.de(
                beneficiario(LocalDate.of(1968, 5, 10)), HOJE);

        assertThat(resposta.toString()).doesNotContain("52998224725", "45872213300", "12987654321");
    }
}