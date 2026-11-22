package fatec.fusex.nexus.preGuia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.TipoDependente;
import fatec.fusex.nexus.usuario.Usuario;

class AcessoPacientePodeAgirTest {

    private static final long TITULAR = 1L;
    private static final long RESPONSAVEL = 2L;
    private static final long MAIOR = 3L;

    private final Usuario titular = usuario(TITULAR);
    private final Usuario responsavel = usuario(RESPONSAVEL);
    private final Usuario maior = usuario(MAIOR);

    @Test
    void titularAgeNaPreGuiaSemDependenteENoMenorDeSuaGuarda() {
        assertThat(AcessoPaciente.podeAgir(titular, preGuia(null))).isTrue();
        assertThat(AcessoPaciente.podeAgir(titular, preGuia(dependente(TipoDependente.MENOR_TITULAR, null, null))))
                .isTrue();
    }

    @Test
    void titularNaoAgePeloMaiorNemPeloMenorSobResponsavel() {
        assertThat(AcessoPaciente.podeAgir(titular, preGuia(dependente(TipoDependente.MAIOR, maior, null))))
                .isFalse();
        assertThat(AcessoPaciente.podeAgir(titular,
                preGuia(dependente(TipoDependente.MENOR_RESPONSAVEL, null, responsavel)))).isFalse();
    }

    @Test
    void responsavelExternoAgeSoPeloMenorQueAcompanha() {
        assertThat(AcessoPaciente.podeAgir(responsavel,
                preGuia(dependente(TipoDependente.MENOR_RESPONSAVEL, null, responsavel)))).isTrue();
        assertThat(AcessoPaciente.podeAgir(responsavel, preGuia(null))).isFalse();
    }

    @Test
    void maiorAgeSoPorSi() {
        assertThat(AcessoPaciente.podeAgir(maior, preGuia(dependente(TipoDependente.MAIOR, maior, null))))
                .isTrue();
        assertThat(AcessoPaciente.podeAgir(maior, preGuia(null))).isFalse();
    }

    private static Usuario usuario(long id) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        return u;
    }

    private PreGuia preGuia(Dependente dependente) {
        Beneficiario b = new Beneficiario();
        b.setUsuarioId(TITULAR);
        PreGuia p = new PreGuia();
        p.setBeneficiario(b);
        p.setDependente(dependente);
        return p;
    }

    private static Dependente dependente(TipoDependente tipo, Usuario conta, Usuario responsavel) {
        Dependente d = new Dependente();
        d.setTipo(tipo);
        d.setUsuario(conta);
        d.setResponsavel(responsavel);
        d.setDataNascimento(LocalDate.of(2010, 1, 1));
        return d;
    }
}
