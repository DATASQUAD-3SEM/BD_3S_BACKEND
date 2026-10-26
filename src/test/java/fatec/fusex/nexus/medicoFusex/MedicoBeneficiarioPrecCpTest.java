package fatec.fusex.nexus.medicoFusex;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
import fatec.fusex.nexus.medicoFusex.dto.BeneficiarioMedicoResponse;
import fatec.fusex.nexus.usuario.PerfilUsuario;
import fatec.fusex.nexus.usuario.Usuario;
import fatec.fusex.nexus.usuario.UsuarioRepository;

/**
 * Cobertura HTTP do contrato T2-BE1 (GET /medicos/beneficiarios?precCp=),
 * usando o BeneficiarioService real e apenas o repositorio mockado.
 */
class MedicoBeneficiarioPrecCpTest {

    private static final String LOGIN_MEDICO = "11144477735";
    private static final String PREC_CP = "45872213300";
    private static final String ENDPOINT = "/medicos/beneficiarios";

    private MockMvc mockMvc;

    @Mock
    private BeneficiarioRepository beneficiarioRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    @Mock
    private MedicoFusexService medicoService;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);

        BeneficiarioService beneficiarioService =
                new BeneficiarioService(beneficiarioRepository, usuarioRepository, simulacaoLogin);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new MedicoBeneficiarioController(beneficiarioService, simulacaoLogin, medicoService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();

        when(simulacaoLogin.loginAtual()).thenReturn(LOGIN_MEDICO);
        when(medicoService.validarAcessoMedico(LOGIN_MEDICO)).thenReturn(new MedicoFusex());
    }

    @Test
    void buscar_precCpSemMascara_retorna200ComDadosDoBeneficiario() throws Exception {
        when(beneficiarioRepository.findWithUsuarioByPrecCp(PREC_CP)).thenReturn(Optional.of(beneficiario(true)));

        mockMvc.perform(get(ENDPOINT).param("precCp", PREC_CP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Antonio Carlos Ferreira"))
                .andExpect(jsonPath("$.grupo").value(BeneficiarioMedicoResponse.GRUPO))
                .andExpect(jsonPath("$.cpf").value("52998224725"))
                .andExpect(jsonPath("$.telefone").value("12987654321"))
                .andExpect(jsonPath("$.idade").exists())
                .andExpect(jsonPath("$.faixaEtaria").exists());
    }

    @Test
    void buscar_precCpComMascara_retorna200EBuscaValorNormalizado() throws Exception {
        when(beneficiarioRepository.findWithUsuarioByPrecCp(PREC_CP)).thenReturn(Optional.of(beneficiario(true)));

        mockMvc.perform(get(ENDPOINT).param("precCp", "458.722.133-00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Antonio Carlos Ferreira"));

        verify(beneficiarioRepository).findWithUsuarioByPrecCp(PREC_CP);
    }

    @Test
    void buscar_beneficiarioInexistente_retorna404() throws Exception {
        when(beneficiarioRepository.findWithUsuarioByPrecCp(PREC_CP)).thenReturn(Optional.empty());

        mockMvc.perform(get(ENDPOINT).param("precCp", PREC_CP))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscar_beneficiarioInativo_retorna404() throws Exception {
        when(beneficiarioRepository.findWithUsuarioByPrecCp(PREC_CP)).thenReturn(Optional.of(beneficiario(false)));

        mockMvc.perform(get(ENDPOINT).param("precCp", PREC_CP))
                .andExpect(status().isNotFound());
    }

    @Test
    void buscar_formatoInvalido_retorna400SemIrAoBanco() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("precCp", "123"))
                .andExpect(status().isBadRequest());

        verify(beneficiarioRepository, never()).findWithUsuarioByPrecCp(anyString());
    }

    @Test
    void buscar_semPrecCp_retorna400() throws Exception {
        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isBadRequest());

        verify(beneficiarioRepository, never()).findWithUsuarioByPrecCp(anyString());
    }

    @Test
    void buscar_perfilNaoMedico_retorna403SemIrAoBanco() throws Exception {
        when(medicoService.validarAcessoMedico(LOGIN_MEDICO))
                .thenThrow(new AcessoNegadoException("Acesso restrito a medicos"));

        mockMvc.perform(get(ENDPOINT).param("precCp", PREC_CP))
                .andExpect(status().isForbidden());

        verify(beneficiarioRepository, never()).findWithUsuarioByPrecCp(anyString());
    }

    private static Beneficiario beneficiario(boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setLogin("52998224725");
        usuario.setPerfil(PerfilUsuario.BENEFICIARIO);
        usuario.setAtivo(ativo);

        Beneficiario b = new Beneficiario();
        b.setUsuarioId(7L);
        b.setUsuario(usuario);
        b.setNome("Antonio Carlos Ferreira");
        b.setPrecCp(PREC_CP);
        b.setDataNascimento(LocalDate.of(1968, 3, 15));
        b.setTelefone("12987654321");
        return b;
    }
}