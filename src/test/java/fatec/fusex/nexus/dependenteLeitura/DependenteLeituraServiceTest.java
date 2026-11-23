package fatec.fusex.nexus.dependenteLeitura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.DependenteRepository;
import fatec.fusex.nexus.dependente.TipoDependente;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/** T31-BE5 (SCRUM-274): visoes de leitura de dependentes. */
class DependenteLeituraServiceTest {

    private static final long TITULAR = 1L;
    private static final long RESPONSAVEL = 41L;

    @Test
    void titularVeOsVigentesEPodeCriarPreGuiaSoNoMenorSobSuaGuarda() {
        UsuarioLogado logado = mock(UsuarioLogado.class);
        DependenteRepository repo = mock(DependenteRepository.class);
        Usuario titular = usuario(TITULAR, PerfilUsuario.BENEFICIARIO);
        when(logado.exigirPerfilAtivo(PerfilUsuario.BENEFICIARIO)).thenReturn(titular);

        Dependente miguel = dependente("Miguel", TipoDependente.MENOR_TITULAR, LocalDate.now().plusYears(20));
        Dependente vencido = dependente("Pedro", TipoDependente.MENOR_TITULAR, LocalDate.now().minusDays(1));
        Dependente sofia = dependente("Sofia", TipoDependente.MENOR_RESPONSAVEL, LocalDate.now().plusYears(10));
        when(repo.findByTitularUsuarioIdOrderByIdDependenteAsc(TITULAR)).thenReturn(List.of(miguel, vencido, sofia));

        List<DependenteLeituraService.DependenteDoTitularResponse> lista =
                new DependenteLeituraService(logado, repo).dependentesDoTitular();

        assertThat(lista).extracting(DependenteLeituraService.DependenteDoTitularResponse::nome)
                .containsExactly("Miguel", "Sofia");
        assertThat(lista).extracting(DependenteLeituraService.DependenteDoTitularResponse::podeCriarPreGuia)
                .containsExactly(true, false);
    }

    @Test
    void responsavelVeSoOsMenoresVigentesQueAcompanha() {
        UsuarioLogado logado = mock(UsuarioLogado.class);
        DependenteRepository repo = mock(DependenteRepository.class);
        Usuario responsavel = usuario(RESPONSAVEL, PerfilUsuario.RESPONSAVEL_EXTERNO);
        when(logado.exigirPerfilAtivo(PerfilUsuario.RESPONSAVEL_EXTERNO)).thenReturn(responsavel);

        Dependente sofia = dependente("Sofia", TipoDependente.MENOR_RESPONSAVEL, LocalDate.now().plusYears(10));
        Dependente vencida = dependente("Ana", TipoDependente.MENOR_RESPONSAVEL, LocalDate.now().minusDays(3));
        when(repo.findByResponsavelIdUsuarioAndTipo(RESPONSAVEL, TipoDependente.MENOR_RESPONSAVEL))
                .thenReturn(List.of(sofia, vencida));

        List<DependenteLeituraService.MenorAcompanhadoResponse> lista =
                new DependenteLeituraService(logado, repo).menoresDoResponsavel();

        assertThat(lista).extracting(DependenteLeituraService.MenorAcompanhadoResponse::nome).containsExactly("Sofia");
        assertThat(lista.get(0).titularNome()).isEqualTo("Titular");
        assertThat(lista.get(0).precCp()).isEqualTo(sofia.getPrecCp());
    }

    private static Usuario usuario(long id, PerfilUsuario perfil) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        u.setPerfil(perfil);
        return u;
    }

    private static Dependente dependente(String nome, TipoDependente tipo, LocalDate vigenciaFim) {
        Beneficiario titular = new Beneficiario();
        titular.setUsuarioId(TITULAR);
        titular.setNome("Titular");
        titular.setPrecCp("45872213300");
        Dependente d = new Dependente();
        d.setNome(nome);
        d.setTipo(tipo);
        d.setTitular(titular);
        d.setVigenciaFim(vigenciaFim);
        d.setDataNascimento(LocalDate.now().minusYears(5));
        d.setPrecCp("12345678901");
        return d;
    }
}
