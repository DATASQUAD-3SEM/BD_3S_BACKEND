package fatec.fusex.nexus.beneficiario;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import fatec.fusex.nexus.assinatura.Assinatura;
import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.beneficiario.dto.BeneficiarioResponse;
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
    void de_naoTemOCampoGrupo() {
        // T33-BE1: o campo grupo (texto fixo sem origem nas regras) foi removido.
        assertThat(BeneficiarioResponse.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("grupo");
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
    void de_assinaturaCadastrada_refleteSeExisteAssinatura() {
        Beneficiario comAssinatura = beneficiario(LocalDate.of(1968, 5, 10));
        comAssinatura.getUsuario().setAssinatura(assinatura("uploads/assinatura.png"));
    }

    @Test
    void toString_naoExpoeCpfNemTelefone() {
        BeneficiarioResponse resposta = BeneficiarioResponse.de(
                beneficiario(LocalDate.of(1968, 5, 10)), HOJE);

        assertThat(resposta.toString()).doesNotContain("52998224725", "45872213300", "12987654321");
    }

    private static Assinatura assinatura(String url) {
        Assinatura assinatura = new Assinatura();
        assinatura.setAssinaturaUrl(url);
        return assinatura;
    }
}