package fatec.fusex.nexus.chefeBeneficiario.dependentes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.DependenteRepository;
import fatec.fusex.nexus.dependente.ResponsavelExterno;
import fatec.fusex.nexus.dependente.ResponsavelExternoRepository;
import fatec.fusex.nexus.dependente.TipoDependente;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;

/** T31-BE1 (SCRUM-270): lista de dependentes do titular e proximo Prec CP livre. */
class ChefeDependenteServiceTest {

    private static final long TITULAR = 1L;

    private UsuarioLogado usuarioLogado;
    private BeneficiarioRepository beneficiarioRepository;
    private DependenteRepository dependenteRepository;
    private ResponsavelExternoRepository responsavelExternoRepository;
    private ChefeDependenteService service;
    private Beneficiario titular;

    @BeforeEach
    void configurar() {
        usuarioLogado = mock(UsuarioLogado.class);
        beneficiarioRepository = mock(BeneficiarioRepository.class);
        dependenteRepository = mock(DependenteRepository.class);
        responsavelExternoRepository = mock(ResponsavelExternoRepository.class);
        service = new ChefeDependenteService(usuarioLogado, beneficiarioRepository, dependenteRepository,
                responsavelExternoRepository);

        Usuario chefe = new Usuario();
        chefe.setIdUsuario(99L);
        chefe.setPerfil(PerfilUsuario.CHEFE_FUSEX);
        when(usuarioLogado.exigirPerfilAtivo(PerfilUsuario.CHEFE_FUSEX)).thenReturn(chefe);

        titular = new Beneficiario();
        titular.setUsuarioId(TITULAR);
        titular.setNome("Antonio Titular");
        titular.setPrecCp("45872213300");
        when(beneficiarioRepository.findById(TITULAR)).thenReturn(Optional.of(titular));
    }

    @Test
    void listaVigentesAntesDosVencidosComOsResponsaveisDeCadaTipo() {
        ResponsavelExterno helena = new ResponsavelExterno();
        helena.setUsuarioId(41L);
        helena.setNome("Helena Costa");
        when(responsavelExternoRepository.findById(41L)).thenReturn(Optional.of(helena));

        Dependente vencido = dependente(1L, "Carla Vencida", TipoDependente.MAIOR, LocalDate.now().minusDays(1));
        Dependente miguel = dependente(2L, "Miguel Ferreira", TipoDependente.MENOR_TITULAR, LocalDate.now().plusYears(20));
        Dependente sofia = dependente(3L, "Sofia Ferreira", TipoDependente.MENOR_RESPONSAVEL, LocalDate.now().plusYears(10));
        sofia.setResponsavel(usuario(41L));
        when(dependenteRepository.findByTitularUsuarioIdOrderByIdDependenteAsc(TITULAR))
                .thenReturn(List.of(vencido, miguel, sofia));

        DependentesDoTitularResponse resposta = service.listarDoTitular(TITULAR);

        assertThat(resposta.dependentes()).extracting(DependenteItemResponse::nome)
                .containsExactly("Miguel Ferreira", "Sofia Ferreira", "Carla Vencida");
        assertThat(resposta.dependentes().get(0).responsaveis())
                .containsExactly(new ResponsavelDependenteResponse(TITULAR, "Antonio Titular", "TITULAR"));
        assertThat(resposta.dependentes().get(1).responsaveis())
                .containsExactly(new ResponsavelDependenteResponse(41L, "Helena Costa", "EXTERNO"));
        assertThat(resposta.dependentes().get(2).vigenciaVencida()).isTrue();
        // CPF e telefone vao na lista para a edicao do chefe vir preenchida (T31-FE3).
        assertThat(resposta.dependentes().get(0).cpf()).isEqualTo("39053344705");
        assertThat(resposta.dependentes().get(0).telefone()).isEqualTo("12999990000");
        assertThat(resposta.dependentes().get(2).responsaveis()).isEmpty();
    }

    @Test
    void proximoPrecCpPulaOsJaOcupados() {
        Dependente existente = dependente(1L, "Miguel Ferreira", TipoDependente.MENOR_TITULAR, LocalDate.now().plusYears(20));
        existente.setPrecCp("45872213301");
        when(dependenteRepository.findByPrecCpStartingWith("458722133")).thenReturn(List.of(existente));

        assertThat(service.proximoPrecCp(TITULAR).precCp()).isEqualTo("45872213302");
    }

    private static Usuario usuario(long id) {
        Usuario u = new Usuario();
        u.setIdUsuario(id);
        return u;
    }

    private static Dependente dependente(long id, String nome, TipoDependente tipo, LocalDate vigenciaFim) {
        Beneficiario t = new Beneficiario();
        t.setUsuarioId(TITULAR);
        t.setNome("Antonio Titular");
        t.setPrecCp("45872213300");
        Dependente d = new Dependente();
        d.setIdDependente(id);
        d.setNome(nome);
        d.setTitular(t);
        d.setTipo(tipo);
        d.setVigenciaFim(vigenciaFim);
        d.setDataNascimento(LocalDate.of(2010, 1, 1));
        d.setCpf("39053344705");
        d.setTelefone("12999990000");
        d.setPrecCp("1234567890" + id);
        return d;
    }
}
