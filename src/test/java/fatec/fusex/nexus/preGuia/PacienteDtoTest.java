package fatec.fusex.nexus.preGuia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.TipoDependente;
import fatec.fusex.nexus.preGuia.dto.PacienteDto;
import fatec.fusex.nexus.usuario.Usuario;

/** Correcao T30-BE2/BE3: dados do paciente nas listas, para o responsavel legal e para o titular. */
class PacienteDtoTest {

    private final Beneficiario titular = beneficiario("Joao");

    @Test
    void titularVeOPropriaPreGuiaComoVoceSemMenor() {
        PreGuia pg = preGuia(null);

        PacienteDto p = PacienteDto.de(pg, usuario(1L));

        assertThat(p.dependenteId()).isNull();
        assertThat(p.relacao()).isEqualTo("Titular");
        assertThat(p.menor()).isFalse();
        assertThat(p.voceResponsavel()).isFalse();
    }

    @Test
    void responsavelLegalDoMenorTemVoceResponsavel() {
        Usuario responsavel = usuario(9L);
        PreGuia pg = preGuia(dependente(TipoDependente.MENOR_RESPONSAVEL, responsavel, LocalDate.now().plusYears(2)));

        PacienteDto p = PacienteDto.de(pg, responsavel);

        assertThat(p.nome()).isEqualTo("Miguel");
        assertThat(p.menor()).isTrue();
        assertThat(p.voceResponsavel()).isTrue();
        assertThat(p.vigenciaFim()).isNotNull();
    }

    @Test
    void titularDoMenorNaoEhOResponsavelLegal() {
        Usuario responsavel = usuario(9L);
        PreGuia pg = preGuia(dependente(TipoDependente.MENOR_TITULAR, null, LocalDate.now().plusYears(2)));

        PacienteDto p = PacienteDto.de(pg, usuario(1L));

        assertThat(p.menor()).isTrue();
        assertThat(p.voceResponsavel()).isFalse();
    }

    @Test
    void maiorDeIdadeNaoEMenorNemTemResponsavel() {
        PreGuia pg = preGuia(dependente(TipoDependente.MAIOR, null, null));

        PacienteDto p = PacienteDto.de(pg, usuario(1L));

        assertThat(p.menor()).isFalse();
        assertThat(p.voceResponsavel()).isFalse();
    }

    private PreGuia preGuia(Dependente dependente) {
        PreGuia pg = new PreGuia();
        pg.setBeneficiario(titular);
        pg.setDependente(dependente);
        return pg;
    }

    private Dependente dependente(TipoDependente tipo, Usuario responsavel, LocalDate vigenciaFim) {
        Dependente d = new Dependente();
        d.setIdDependente(7L);
        d.setNome("Miguel");
        d.setVinculo("Filho");
        d.setTipo(tipo);
        d.setResponsavel(responsavel);
        d.setVigenciaFim(vigenciaFim);
        return d;
    }

    private Beneficiario beneficiario(String nome) {
        Beneficiario b = new Beneficiario();
        b.setNome(nome);
        return b;
    }

    private Usuario usuario(Long id) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        return u;
    }
}
