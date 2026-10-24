package fatec.fusex.nexus.medicoFusex;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import fatec.fusex.nexus.beneficiario.Beneficiario;
import fatec.fusex.nexus.beneficiario.BeneficiarioService;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.usuario.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class MedicoBeneficiarioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BeneficiarioService beneficiarioService;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    @Mock
    private MedicoFusexService medicoService;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MedicoBeneficiarioController(beneficiarioService, simulacaoLogin, medicoService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void buscar_comMedicoLogado_retorna200() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn("11144477735");
        when(medicoService.validarAcessoMedico("11144477735")).thenReturn(new MedicoFusex());

        // Cria um beneficiário fictício para o service retornar
        Beneficiario b = new Beneficiario();
        b.setUsuarioId(1L);
        b.setNome("Carlos Menezes");
        b.setPrecCp("12345678900");
        b.setDataNascimento(LocalDate.of(1978, 3, 14));
        b.setTelefone("12991234567");

        // Cria o usuário para o beneficiário
        Usuario u = new Usuario();
        u.setLogin("98765432100");
        b.setUsuario(u);

        when(beneficiarioService.buscarAtivoPorPrecCp(anyString())).thenReturn(b);

        mockMvc.perform(get("/medicos/beneficiarios")
                        .param("precCp", "12345678900")
                        .header("X-Usuario-Login", "11144477735"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carlos Menezes"));
    }

    @Test
    void buscar_semPerfilMedico_retorna403() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn("52998224725");
        when(medicoService.validarAcessoMedico("52998224725"))
                .thenThrow(new AcessoNegadoException("Acesso restrito a medicos"));

        mockMvc.perform(get("/medicos/beneficiarios")
                        .param("precCp", "12345678900")
                        .header("X-Usuario-Login", "52998224725"))
                .andExpect(status().isForbidden());
    }

    @Test
    void buscar_semUsuario_retorna401() throws Exception {
        when(simulacaoLogin.loginAtual()).thenThrow(new NaoAutenticadoException("Nenhum usuario logado"));

        mockMvc.perform(get("/medicos/beneficiarios")
                        .param("precCp", "12345678900"))
                .andExpect(status().isUnauthorized());
    }
}