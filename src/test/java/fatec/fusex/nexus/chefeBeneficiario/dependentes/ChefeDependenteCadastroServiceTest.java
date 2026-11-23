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
import fatec.fusex.nexus.dependente.ResponsavelExterno;
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

    @Test
    void maiorReaproveitaContaDeResponsavelExternoEMudaOPerfil() {
        Usuario existente = new Usuario();
        existente.setIdUsuario(77L);
        existente.setLogin(CPF_MAIOR);
        existente.setPerfil(PerfilUsuario.RESPONSAVEL_EXTERNO);
        when(usuarioRepository.findByLogin(CPF_MAIOR)).thenReturn(Optional.of(existente));
        CriarDependenteRequest req = new CriarDependenteRequest("Maria Adulta", LocalDate.of(1985, 2, 8), CPF_MAIOR,
                null, "12345678901", "Conjuge", "VITALICIA", null, null, null);

        DependenteItemResponse criado = service.criar(TITULAR, req);

        assertThat(criado.tipo()).isEqualTo(TipoDependente.MAIOR);
        assertThat(existente.getPerfil()).isEqualTo(PerfilUsuario.BENEFICIARIO);
        assertThat(existente.getIdUsuario()).isEqualTo(77L);
        verify(usuarioRepository).save(existente);
        verify(passwordEncoder, org.mockito.Mockito.never()).encode(any());
    }

    @Test
    void menorComOCpfDoTitularEhRecusadoSemGravarNada() {
        Usuario titular = new Usuario();
        titular.setLogin(CPF_MENOR);
        titular.setPerfil(PerfilUsuario.BENEFICIARIO);
        when(usuarioRepository.findByLogin(CPF_MENOR)).thenReturn(Optional.of(titular));
        CriarDependenteRequest req = requisicao(LocalDate.now().minusYears(5), "DETERMINADA",
                LocalDate.now().plusYears(17), new CriarDependenteRequest.ResponsavelRequest("TITULAR", null, null, null), null);

        assertThatThrownBy(() -> service.criar(TITULAR, req))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("Este CPF já pertence a outra pessoa cadastrada no Nexus.");
        verify(dependenteRepository, org.mockito.Mockito.never()).save(any(Dependente.class));
    }

    @Test
    void menorComOCpfDeUmMedicoEhRecusado() {
        Usuario medico = new Usuario();
        medico.setLogin(CPF_MENOR);
        medico.setPerfil(PerfilUsuario.MEDICO);
        when(usuarioRepository.findByLogin(CPF_MENOR)).thenReturn(Optional.of(medico));
        CriarDependenteRequest req = requisicao(LocalDate.now().minusYears(5), "DETERMINADA",
                LocalDate.now().plusYears(17), new CriarDependenteRequest.ResponsavelRequest("TITULAR", null, null, null), null);

        assertThatThrownBy(() -> service.criar(TITULAR, req))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("Este CPF já pertence a outra pessoa cadastrada no Nexus.");
        verify(dependenteRepository, org.mockito.Mockito.never()).save(any(Dependente.class));
    }

    @Test
    void cpfDeOutroDependenteEhRecusado() {
        when(dependenteRepository.existsByCpf(CPF_MENOR)).thenReturn(true);
        CriarDependenteRequest req = requisicao(LocalDate.now().minusYears(5), "DETERMINADA",
                LocalDate.now().plusYears(17), new CriarDependenteRequest.ResponsavelRequest("TITULAR", null, null, null), null);

        assertThatThrownBy(() -> service.criar(TITULAR, req))
                .isInstanceOf(DadosInvalidosException.class)
                .hasMessageContaining("Este CPF já pertence a outra pessoa cadastrada no Nexus.");
    }

    @Test
    void maiorComCpfDeOutroPerfilEhRecusado() {
        Usuario medico = new Usuario();
        medico.setLogin(CPF_MAIOR);
        medico.setPerfil(PerfilUsuario.MEDICO);
        when(usuarioRepository.findByLogin(CPF_MAIOR)).thenReturn(Optional.of(medico));
        CriarDependenteRequest req = new CriarDependenteRequest("Maria Adulta", LocalDate.of(1985, 2, 8), CPF_MAIOR,
                null, "12345678901", "Conjuge", "VITALICIA", null, null, SENHA);

        assertThatThrownBy(() -> service.criar(TITULAR, req)).isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void responsavelReaproveitaDependenteMaiorVitalicio() {
        Usuario maior = new Usuario();
        maior.setIdUsuario(88L);
        maior.setLogin(CPF_RESPONSAVEL);
        maior.setPerfil(PerfilUsuario.BENEFICIARIO);
        when(usuarioRepository.findByLogin(CPF_RESPONSAVEL)).thenReturn(Optional.of(maior));
        when(dependenteRepository.findByUsuarioIdUsuario(88L)).thenReturn(Optional.of(dependenteExistente(TipoDependente.MAIOR, null)));
        ResponsavelExterno externo = new ResponsavelExterno();
        externo.setUsuarioId(88L);
        externo.setNome("Pessoa Adulta");
        when(responsavelExternoRepository.findById(88L)).thenReturn(Optional.of(externo));
        CriarDependenteRequest req = requisicao(LocalDate.now().minusYears(5), "DETERMINADA", LocalDate.now().plusYears(17),
                new CriarDependenteRequest.ResponsavelRequest("OUTRA_PESSOA", CPF_RESPONSAVEL, "Pessoa Adulta", "12998765432"),
                null);

        DependenteItemResponse criado = service.criar(TITULAR, req);

        assertThat(criado.tipo()).isEqualTo(TipoDependente.MENOR_RESPONSAVEL);
        assertThat(maior.getPerfil()).isEqualTo(PerfilUsuario.BENEFICIARIO);
        verify(responsavelExternoRepository).save(org.mockito.ArgumentMatchers.argThat(e -> e.getUsuarioId() == 88L));
    }

    @Test
    void responsavelComDependenteDeVigenciaDeterminadaEhRecusado() {
        Usuario maior = new Usuario();
        maior.setIdUsuario(88L);
        maior.setLogin(CPF_RESPONSAVEL);
        maior.setPerfil(PerfilUsuario.BENEFICIARIO);
        when(usuarioRepository.findByLogin(CPF_RESPONSAVEL)).thenReturn(Optional.of(maior));
        when(dependenteRepository.findByUsuarioIdUsuario(88L))
                .thenReturn(Optional.of(dependenteExistente(TipoDependente.MAIOR, LocalDate.now().plusYears(2))));
        CriarDependenteRequest req = requisicao(LocalDate.now().minusYears(5), "DETERMINADA", LocalDate.now().plusYears(17),
                new CriarDependenteRequest.ResponsavelRequest("OUTRA_PESSOA", CPF_RESPONSAVEL, "Pessoa", "12998765432"),
                null);

        assertThatThrownBy(() -> service.criar(TITULAR, req)).isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void responsavelQueEhTitularEhRecusado() {
        Usuario titular = new Usuario();
        titular.setIdUsuario(88L);
        titular.setLogin(CPF_RESPONSAVEL);
        titular.setPerfil(PerfilUsuario.BENEFICIARIO);
        when(usuarioRepository.findByLogin(CPF_RESPONSAVEL)).thenReturn(Optional.of(titular));
        when(beneficiarioRepository.existsById(88L)).thenReturn(true);
        CriarDependenteRequest req = requisicao(LocalDate.now().minusYears(5), "DETERMINADA", LocalDate.now().plusYears(17),
                new CriarDependenteRequest.ResponsavelRequest("OUTRA_PESSOA", CPF_RESPONSAVEL, "Titular", "12998765432"),
                null);

        assertThatThrownBy(() -> service.criar(TITULAR, req)).isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void consultaDeContaClassificaOCpfSemGravarNada() {
        when(usuarioRepository.findByLogin(CPF_RESPONSAVEL)).thenReturn(Optional.empty());
        assertThat(service.consultarConta(CPF_RESPONSAVEL)).isEqualTo(new ContaPorCpfResponse("NOVA", null));

        Usuario externo = new Usuario();
        externo.setIdUsuario(41L);
        externo.setPerfil(PerfilUsuario.RESPONSAVEL_EXTERNO);
        ResponsavelExterno helena = new ResponsavelExterno();
        helena.setUsuarioId(41L);
        helena.setNome("Helena Costa");
        when(usuarioRepository.findByLogin(CPF_RESPONSAVEL)).thenReturn(Optional.of(externo));
        when(responsavelExternoRepository.findById(41L)).thenReturn(Optional.of(helena));
        assertThat(service.consultarConta(CPF_RESPONSAVEL))
                .isEqualTo(new ContaPorCpfResponse("RESPONSAVEL_EXTERNO", "Helena Costa"));

        Usuario maior = new Usuario();
        maior.setIdUsuario(88L);
        maior.setPerfil(PerfilUsuario.BENEFICIARIO);
        when(usuarioRepository.findByLogin(CPF_RESPONSAVEL)).thenReturn(Optional.of(maior));
        when(dependenteRepository.findByUsuarioIdUsuario(88L)).thenReturn(Optional.of(dependenteExistente(TipoDependente.MAIOR, null)));
        assertThat(service.consultarConta(CPF_RESPONSAVEL).situacao()).isEqualTo("DEPENDENTE_MAIOR_VITALICIO");

        when(beneficiarioRepository.existsById(88L)).thenReturn(true);
        assertThat(service.consultarConta(CPF_RESPONSAVEL)).isEqualTo(new ContaPorCpfResponse("OUTRA", null));

        verify(usuarioRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void editarMaiorTrocaVigenciaESemAlterarResponsavel() {
        Dependente maior = dependenteExistente(TipoDependente.MAIOR, null);
        when(dependenteRepository.findById(7L)).thenReturn(Optional.of(maior));

        DependenteItemResponse editado = service.editar(7L,
                new EditarDependenteRequest(null, null, null, null, "DETERMINADA", LocalDate.now().plusYears(2)));

        assertThat(editado.vigenciaFim()).isEqualTo(LocalDate.now().plusYears(2));
        assertThat(editado.responsaveis()).isEmpty();
    }

    @Test
    void editarMenorParaVitalicioEhRecusado() {
        Dependente menor = dependenteExistente(TipoDependente.MENOR_TITULAR, LocalDate.now().plusYears(10));
        when(dependenteRepository.findById(7L)).thenReturn(Optional.of(menor));

        assertThatThrownBy(() -> service.editar(7L,
                new EditarDependenteRequest(null, null, null, null, "VITALICIA", null)))
                .isInstanceOf(DadosInvalidosException.class);
    }

    @Test
    void editarPrecCpJaOcupadoEhRecusado() {
        Dependente menor = dependenteExistente(TipoDependente.MENOR_TITULAR, LocalDate.now().plusYears(10));
        when(dependenteRepository.findById(7L)).thenReturn(Optional.of(menor));
        Dependente outro = new Dependente();
        outro.setIdDependente(99L);
        when(dependenteRepository.findByPrecCp("12345678999")).thenReturn(Optional.of(outro));

        assertThatThrownBy(() -> service.editar(7L,
                new EditarDependenteRequest(null, null, "12345678999", null, null, null)))
                .isInstanceOf(DadosInvalidosException.class);
    }

    private Dependente dependenteExistente(TipoDependente tipo, LocalDate fim) {
        Beneficiario titular = new Beneficiario();
        titular.setUsuarioId(TITULAR);
        titular.setNome("Antonio Titular");
        titular.setPrecCp("45872213300");
        Dependente d = new Dependente();
        d.setIdDependente(7L);
        d.setTitular(titular);
        d.setNome("Dependente Existente");
        d.setCpf(CPF_MAIOR);
        d.setPrecCp("12345678901");
        d.setDataNascimento(LocalDate.of(1985, 2, 8));
        d.setTipo(tipo);
        d.setVigenciaFim(fim);
        return d;
    }

    private static CriarDependenteRequest requisicao(LocalDate nascimento, String tipoVigencia, LocalDate vigenciaFim,
                                                     CriarDependenteRequest.ResponsavelRequest responsavel, String senha) {
        return new CriarDependenteRequest("Dependente Teste", nascimento, CPF_MENOR, null, "12345678902",
                "Filho", tipoVigencia, vigenciaFim, responsavel, senha);
    }
}
