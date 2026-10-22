package fatec.fusex.nexus.beneficiario;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.beneficiario.dto.BeneficiarioMedicoResponse;
import fatec.fusex.nexus.beneficiario.dto.BeneficiarioResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

class BeneficiarioResponseTest {

    @Test
    void de_grupoESempreOTextoFixoDoFusex_eNaoOPerfil() {
        Usuario usuario = new Usuario();
        usuario.setLogin("52998224725");
        usuario.setPerfil(PerfilUsuario.BENEFICIARIO);

        Beneficiario b = new Beneficiario();
        b.setUsuario(usuario);
        b.setNome("Antonio Carlos Ferreira");
        b.setPrecCp("45872213300");
        b.setDataNascimento(LocalDate.of(1978, 5, 10));
        b.setTelefone("12987654321");

        BeneficiarioResponse resposta = BeneficiarioResponse.de(b);

        assertThat(resposta.grupo()).isEqualTo(BeneficiarioMedicoResponse.GRUPO);
        assertThat(resposta.grupo()).isEqualTo("Ex\u00e9rcito Brasileiro \u00b7 FUSEX");
    }
}
