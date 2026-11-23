package fatec.fusex.nexus.medicoFusex;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioRepository;
import fatec.fusex.nexus.beneficiario.BeneficiarioService;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.dependente.Dependente;
import fatec.fusex.nexus.dependente.DependenteRepository;
import fatec.fusex.nexus.dependente.ResponsavelExterno;
import fatec.fusex.nexus.dependente.ResponsavelExternoRepository;
import fatec.fusex.nexus.dependente.TipoDependente;
import fatec.fusex.nexus.medicoFusex.dto.BeneficiarioMedicoResponse;
import fatec.fusex.nexus.medicoFusex.MedicoFusex;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/** T30-BE7 (SCRUM-264): busca do medico por Prec CP de dependente. */
class MedicoBeneficiarioDependenteTest {

    private static final String LOGIN_MEDICO = "11144477735";
    private static final String PREC_MENOR = "12345678902";
    private static final String PREC_EXPIRADO = "12345678999";

    private MockMvc mockMvc;

    @Mock private BeneficiarioRepository beneficiarioRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private SimulacaoLogin simulacaoLogin;
    @Mock private MedicoFusexService medicoService;
    @Mock private DependenteRepository dependenteRepository;
    @Mock private ResponsavelExternoRepository responsavelExternoRepository;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        BeneficiarioService beneficiarioService =
                new BeneficiarioService(beneficiarioRepository, usuarioRepository, simulacaoLogin);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MedicoBeneficiarioController(beneficiarioService, simulacaoLogin, medicoService,
                        dependenteRepository, responsavelExternoRepository,
                        org.mockito.Mockito.mock(org.springframework.transaction.PlatformTransactionManager.class)))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        when(simulacaoLogin.loginAtual()).thenReturn(LOGIN_MEDICO);
        when(medicoService.validarAcessoMedico(LOGIN_MEDICO)).thenReturn(new MedicoFusex());
        when(beneficiarioRepository.findWithUsuarioByPrecCp(any())).thenReturn(Optional.empty());
    }

    @Test
    void menorSobGuardaDoTitularTrazBlocoDoDependenteComOTitularComoResponsavel() throws Exception {
        when(dependenteRepository.findByPrecCp(PREC_MENOR)).thenReturn(Optional.of(
                dependente(TipoDependente.MENOR_TITULAR, LocalDate.of(2048, 3, 20), null)));

        mockMvc.perform(get("/medicos/beneficiarios").param("precCp", PREC_MENOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Miguel Ferreira"))
                .andExpect(jsonPath("$.dependente.menor").value(true))
                .andExpect(jsonPath("$.dependente.titularNome").value("Antonio Titular"))
                .andExpect(jsonPath("$.dependente.responsavel").value("Antonio Titular"));
    }

    @Test
    void menorSobResponsavelExternoTrazONomeDoResponsavel() throws Exception {
        ResponsavelExterno helena = new ResponsavelExterno();
        helena.setUsuarioId(41L);
        helena.setNome("Helena Costa");
        when(responsavelExternoRepository.findById(41L)).thenReturn(Optional.of(helena));
        Dependente sofia = dependente(TipoDependente.MENOR_RESPONSAVEL, LocalDate.of(2039, 5, 14), helena.getUsuarioId());
        when(dependenteRepository.findByPrecCp(PREC_MENOR)).thenReturn(Optional.of(sofia));

        mockMvc.perform(get("/medicos/beneficiarios").param("precCp", PREC_MENOR))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dependente.responsavel").value("Helena Costa"));
    }

    @Test
    void dependenteComVigenciaVencidaRecebe404() throws Exception {
        when(dependenteRepository.findByPrecCp(PREC_EXPIRADO)).thenReturn(Optional.of(
                dependente(TipoDependente.MENOR_TITULAR, LocalDate.now().minusDays(1), null)));

        mockMvc.perform(get("/medicos/beneficiarios").param("precCp", PREC_EXPIRADO))
                .andExpect(status().isNotFound());
    }

    private static Dependente dependente(TipoDependente tipo, LocalDate vigenciaFim, Long responsavelId) {
        Beneficiario titular = new Beneficiario();
        titular.setUsuarioId(1L);
        titular.setNome("Antonio Titular");
        titular.setPrecCp("45872213300");
        Dependente d = new Dependente();
        d.setIdDependente(7L);
        d.setTitular(titular);
        d.setNome("Miguel Ferreira");
        d.setCpf("39053344705");
        d.setPrecCp(PREC_MENOR);
        d.setDataNascimento(LocalDate.of(2026, 3, 21));
        d.setTipo(tipo);
        d.setVigenciaFim(vigenciaFim);
        if (responsavelId != null) {
            fatec.fusex.nexus.usuario.Usuario r = new fatec.fusex.nexus.usuario.Usuario();
            r.setIdUsuario(responsavelId);
            d.setResponsavel(r);
        }
        return d;
    }
}
