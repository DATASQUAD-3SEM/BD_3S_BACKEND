package fatec.fusex.nexus.chefeBeneficiario.dependentes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.common.auth.UsuarioLogado;
import fatec.fusex.nexus.common.exception.DadosInvalidosException;
import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.DependenteRepository;
import fatec.fusex.nexus.dependente.ResponsavelExternoRepository;
import fatec.fusex.nexus.dependente.TipoDependente;
import fatec.fusex.nexus.medicoFusex.MedicoFusexRepository;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/** T31-BE2 (SCRUM-271): cadastro de dependente pelo chefe. */
class ChefeDependenteCadastroServiceTest {

    private static final long TITULAR = 1L;
    private static final String CPF_MENOR = "39053344705";
    private static final String CPF_MAIOR = "52998224725";
    private static final String CPF_RESPONSAVEL = "11144477735";
    private static final String SENHA = "senha-forte-123";

    private BeneficiarioRepository beneficiarioRepository;
    private MedicoFusexRepository medicoRepository;
    private DependenteRepository dependenteRepository;
    private UsuarioRepository usuarioRepository;
    private ResponsavelExternoRepository responsavelExternoRepository;
    private PasswordEncoder passwordEncoder;
    private ChefeDependenteCadastroService service;

    @BeforeEach
    void configurar() {
        UsuarioLogado usuarioLogado = mock(UsuarioLogado.class);
        beneficiarioRepository = mock(BeneficiarioRepository.class);
        medicoRepository = mock(MedicoFusexRepository.class);
        dependenteRepository = mock(DependenteRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        responsavelExternoRepository = mock(ResponsavelExternoRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new ChefeDependenteCadastroService(usuarioLogado, beneficiarioRepository, medicoRepository,
                dependenteRepository, usuarioRepository, responsavelExternoRepository, passwordEncoder);

        Usuario chefe = new Usuario();
        chefe.setIdUsuario(99L);
        when(usuarioLogado.exigirPerfilAtivo(PerfilUsuario.CHEFE_FUSEX)).thenReturn(chefe);

        Beneficiario titular = new Beneficiario();
        titular.setUsuarioId(TITULAR);
        titular.setNome("Antonio Titular");
        titular.setPrecCp("45872213300");
        when(beneficiarioRepository.findById(TITULAR)).thenReturn(Optional.of(titular));

        when(usuarioRepository.findByLogin(anyString())).thenReturn(Optional.empty());
        when(usuarioRepository.save(any())).thenAnswer(i -> {
            Usuario u = i.getArgument(0);
            if (u.getIdUsuario() == null) {
                u.setIdUsuario(50L);
            }
            return u;
        });
        when(passwordEncoder.encode(SENHA)).thenReturn("hash");
    }

    @Test
    void menorComTitularComoResponsavelEhCadastradoComVigenciaDeterminada() {
        CriarDependenteRequest req = requisicao(LocalDate.now().minusYears(5), "DETERMINADA",
                LocalDate.now().plusYears(17), new CriarDependenteRequest.ResponsavelRequest("TITULAR", null, null, null), null);

        DependenteItemResponse criado = service.criar(TITULAR, req);

        assertThat(criado.tipo()).isEqualTo(TipoDependente.MENOR_TITULAR);
        assertThat(criado.responsaveis()).extracting(ResponsavelDependenteResponse::papel).containsExactly("TITULAR");
        verify(dependenteRepository).save(any(Dependente.class));
    }

    @Test
    void menorComResponsavelExternoNovoCriaContaSemPrecCp() {
        fatec.fusex.nexus.dependente.ResponsavelExterno helena = new fatec.fusex.nexus.dependente.ResponsavelExterno();
        helena.setUsuarioId(50L);
        helena.setNome("Helena Costa");
        when(responsavelExternoRepository.findById(50L)).thenReturn(Optional.of(helena));
        CriarDependenteRequest req = requisicao(LocalDate.now().minusYears(5), "DETERMINADA",
                LocalDate.now().plusYears(17),
                new CriarDependenteRequest.ResponsavelRequest("OUTRA_PESSOA", CPF_RESPONSAVEL, "Helena Costa", "12998765432"),
                SENHA);

        DependenteItemResponse criado = service.criar(TITULAR, req);

        assertThat(criado.tipo()).isEqualTo(TipoDependente.MENOR_RESPONSAVEL);
        assertThat(criado.responsaveis()).extracting(ResponsavelDependenteResponse::papel).containsExactly("EXTERNO");
        verify(usuarioRepository).save(org.mockito.ArgumentMatchers.argThat(
                u -> PerfilUsuario.RESPONSAVEL_EXTERNO == u.getPerfil() && CPF_RESPONSAVEL.equals(u.getLogin())));
    }

    @Test
    void menorSemVigenciaDeterminadaEhRecusado() {
        CriarDependenteRequest req = requisicao(LocalDate.now().minusYears(5), "VITALICIA", null,
                new CriarDependenteRequest.ResponsavelRequest("TITULAR", null, null, null), null);

        assertThatThrownBy(() -> service.criar(TITULAR, req)).isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void maiorComResponsavelEhRecusado() {
        CriarDependenteRequest req = requisicao(LocalDate.of(1985, 2, 8), "VITALICIA", null,
                new CriarDependenteRequest.ResponsavelRequest("TITULAR", null, null, null), SENHA);

        assertThatThrownBy(() -> service.criar(TITULAR, req)).isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void precCpJaCadastradoEhRecusado() {
        when(dependenteRepository.findByPrecCp("12345678902")).thenReturn(Optional.of(new Dependente()));
        CriarDependenteRequest req = requisicao(LocalDate.now().minusYears(5), "DETERMINADA",
                LocalDate.now().plusYears(17), new CriarDependenteRequest.ResponsavelRequest("TITULAR", null, null, null), null);

        assertThatThrownBy(() -> service.criar(TITULAR, req)).isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void maiorGanhaContaPropriaComASenhaDoChefe() {
        CriarDependenteRequest req = requisicao(LocalDate.of(1985, 2, 8), "VITALICIA", null, null, SENHA);
        req = new CriarDependenteRequest(req.nome(), req.dataNascimento(), CPF_MAIOR, null, "12345678901",
                "Conjuge", "VITALICIA", null, null, SENHA);

        DependenteItemResponse criado = service.criar(TITULAR, req);

        assertThat(criado.tipo()).isEqualTo(TipoDependente.MAIOR);
        assertThat(criado.temConta()).isTrue();
        verify(usuarioRepository).save(org.mockito.ArgumentMatchers.argThat(
                u -> PerfilUsuario.BENEFICIARIO == u.getPerfil() && CPF_MAIOR.equals(u.getLogin())));
    }

    private static CriarDependenteRequest requisicao(LocalDate nascimento, String tipoVigencia, LocalDate vigenciaFim,
                                                     CriarDependenteRequest.ResponsavelRequest responsavel, String senha) {
        return new CriarDependenteRequest("Dependente Teste", nascimento, CPF_MENOR, null, "12345678902",
                "Filho", tipoVigencia, vigenciaFim, responsavel, senha);
    }
}
