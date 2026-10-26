package fatec.fusex.nexus.medicoFusex;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;
import fatec.fusex.nexus.medicoFusex.dto.MedicoPerfilResponse;

/**
 * Testes HTTP de GET /medicos/me/perfil (T11-BE1).
 */
@WebMvcTest(MedicoPerfilController.class)
@Import(SimulacaoLogin.class)
class MedicoPerfilControllerTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private MedicoPerfilService medicoPerfilService;

    MedicoPerfilControllerTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void meuPerfil_quandoSucesso_retornaDadosDoMedico() throws Exception {
        when(medicoPerfilService.buscarPerfilLogado())
                .thenReturn(new MedicoPerfilResponse(
                        "Carlos Alberto",
                        "Capitao",
                        "52998224725",
                        "45872213300",
                        "CRM-SP-12345",
                        true));

        mockMvc.perform(get("/medicos/me/perfil")
                        .header(
                                SimulacaoLoginFilter.HEADER,
                                "52998224725"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Carlos Alberto"))
                .andExpect(jsonPath("$.patente").value("Capitao"))
                .andExpect(jsonPath("$.cpf").value("52998224725"))
                .andExpect(jsonPath("$.precCp").value("45872213300"))
                .andExpect(jsonPath("$.crm").value("CRM-SP-12345"))
                .andExpect(jsonPath("$.assinaturaCadastrada").value(true));
    }

    @Test
    void meuPerfil_quandoPerfilErrado_retorna403() throws Exception {
        doThrow(new AcessoNegadoException("Acesso restrito a medicos"))
                .when(medicoPerfilService)
                .buscarPerfilLogado();

        mockMvc.perform(get("/medicos/me/perfil")
                        .header(
                                SimulacaoLoginFilter.HEADER,
                                "52998224725"))
                .andExpect(status().isForbidden());
    }

    @Test
    void meuPerfil_quandoCadastroNaoExiste_retorna404() throws Exception {
        doThrow(new RecursoNaoEncontradoException(
                "Perfil medico nao encontrado"))
                .when(medicoPerfilService)
                .buscarPerfilLogado();

        mockMvc.perform(get("/medicos/me/perfil")
                        .header(
                                SimulacaoLoginFilter.HEADER,
                                "52998224725"))
                .andExpect(status().isNotFound());
    }
}