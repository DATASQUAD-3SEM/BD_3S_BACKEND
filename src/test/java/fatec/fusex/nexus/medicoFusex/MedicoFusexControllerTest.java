package fatec.fusex.nexus.medicoFusex;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.ApiExceptionHandler;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.common.exception.RegraDeNegocioException;
import fatec.fusex.nexus.medicoFusex.dto.MedicoMeResponse;
import fatec.fusex.nexus.medicoFusex.dto.ProcedimentoMedicoResponse;
import fatec.fusex.nexus.procedimentoExame.ProcedimentoExameService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Testes HTTP de GET /medicos/me e GET /medicos/procedimentos (T2-BE2 e T2-BE3).
 */
class MedicoFusexControllerTest {

    private MockMvc mockMvc;

    @Mock
    private MedicoFusexService service;

    @Mock
    private ProcedimentoExameService procedimentoService;

    @Mock
    private SimulacaoLogin simulacaoLogin;

    @BeforeEach
    void configurar() {
        MockitoAnnotations.openMocks(this);
        // Instancia o Controller passando os mocks
        mockMvc = MockMvcBuilders
                .standaloneSetup(new MedicoFusexController(service, procedimentoService, simulacaoLogin))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void me_comMedicoLogado_retorna200ComDadosSemPrecCp() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn("11144477735");
        when(service.buscarLogado("11144477735")).thenReturn(
                new MedicoMeResponse("Carlos Eduardo Lima", "Capitao", "123456-SP"));

        mockMvc.perform(get("/medicos/me")
                        .header("X-Usuario-Login", "11144477735"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carlos Eduardo Lima"))
                .andExpect(jsonPath("$.patente").value("Capitao"))
                .andExpect(jsonPath("$.crm").value("123456-SP"))
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.precCp").doesNotExist());
    }

    @Test
    void me_semUsuarioAutenticado_retorna401() throws Exception {
        when(simulacaoLogin.loginAtual()).thenThrow(new NaoAutenticadoException("Nenhum usuario logado"));

        mockMvc.perform(get("/medicos/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_comPerfilErrado_retorna403() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn("52998224725");
        when(service.buscarLogado("52998224725"))
                .thenThrow(new AcessoNegadoException("Acesso restrito a medicos"));

        mockMvc.perform(get("/medicos/me")
                        .header("X-Usuario-Login", "52998224725"))
                .andExpect(status().isForbidden());
    }

    @Test
    void me_medicoSemCadastro_retorna404() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn("11144477735");
        when(service.buscarLogado("11144477735"))
                .thenThrow(new RecursoNaoEncontradoException("Cadastro de medico nao encontrado"));

        mockMvc.perform(get("/medicos/me")
                        .header("X-Usuario-Login", "11144477735"))
                .andExpect(status().isNotFound());
    }

    // ===== GET /medicos/procedimentos - T2-BE3 =====

    @Test
    void procedimentos_comMedicoLogado_retorna200ComCodigoTussENome() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn("11144477735");
        when(procedimentoService.buscar("glic")).thenReturn(
                List.of(new ProcedimentoMedicoResponse("40302040", "Glicose")));

        mockMvc.perform(get("/medicos/procedimentos").param("busca", "glic")
                        .header("X-Usuario-Login", "11144477735"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoTuss").value("40302040"))
                .andExpect(jsonPath("$[0].nome").value("Glicose"))
                .andExpect(jsonPath("$[0].id").doesNotExist());

        verify(service).validarPerfilMedico("11144477735");
    }

    @Test
    void procedimentos_semResultado_retorna200ComListaVazia() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn("11144477735");
        when(procedimentoService.buscar("inexistente")).thenReturn(List.of());

        mockMvc.perform(get("/medicos/procedimentos").param("busca", "inexistente")
                        .header("X-Usuario-Login", "11144477735"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void procedimentos_termoCurto_retorna400() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn("11144477735");
        when(procedimentoService.buscar("a"))
                .thenThrow(new RegraDeNegocioException("A busca deve ter no minimo 2 caracteres"));

        mockMvc.perform(get("/medicos/procedimentos").param("busca", "a")
                        .header("X-Usuario-Login", "11144477735"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void procedimentos_comPerfilErrado_retorna403SemBuscar() throws Exception {
        when(simulacaoLogin.loginAtual()).thenReturn("52998224725");
        doThrow(new AcessoNegadoException("Acesso restrito a medicos"))
                .when(service).validarPerfilMedico("52998224725");

        // termo curto de proposito: o 403 vem antes da validacao da busca
        mockMvc.perform(get("/medicos/procedimentos").param("busca", "a")
                        .header("X-Usuario-Login", "52998224725"))
                .andExpect(status().isForbidden());

        verify(procedimentoService, never()).buscar(any());
    }

    @Test
    void procedimentos_semUsuario_retorna401() throws Exception {
        when(simulacaoLogin.loginAtual()).thenThrow(new NaoAutenticadoException("Nenhum usuario logado"));

        mockMvc.perform(get("/medicos/procedimentos").param("busca", "glic"))
                .andExpect(status().isUnauthorized());

        verify(procedimentoService, never()).buscar(any());
    }
}
