package fatec.fusex.nexus.chefeFusex;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fatec.fusex.nexus.chefeFusex.dto.ChefePerfilResponse;
import fatec.fusex.nexus.common.auth.SimulacaoLogin;
import fatec.fusex.nexus.common.auth.SimulacaoLoginFilter;
import fatec.fusex.nexus.common.exception.AcessoNegadoException;
import fatec.fusex.nexus.common.exception.NaoAutenticadoException;
import fatec.fusex.nexus.common.exception.RecursoNaoEncontradoException;

@WebMvcTest(ChefePerfilController.class)
@Import(SimulacaoLogin.class)
class ChefePerfilControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChefePerfilService chefePerfilService;

    @Test
    void meuPerfil_quandoSucesso_retornaDadosDoChefe() throws Exception {
        when(chefePerfilService.buscarPerfilLogado())
                .thenReturn(new ChefePerfilResponse(
                        "Maria Oliveira",
                        "Major",
                        "chefe-teste",
                        true));

        mockMvc.perform(get("/chefe/me")
                        .header(
                                SimulacaoLoginFilter.HEADER,
                                "chefe-teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Maria Oliveira"))
                .andExpect(jsonPath("$.patente").value("Major"))
                .andExpect(jsonPath("$.login").value("chefe-teste"))
                .andExpect(jsonPath("$.assinaturaCadastrada").value(true));
    }

    @Test
    void meuPerfil_quandoPerfilErrado_retorna403() throws Exception {
        doThrow(new AcessoNegadoException(
                "Acesso restrito ao chefe do FUSEX"))
                .when(chefePerfilService)
                .buscarPerfilLogado();

        mockMvc.perform(get("/chefe/me")
                        .header(
                                SimulacaoLoginFilter.HEADER,
                                "usuario-teste"))
                .andExpect(status().isForbidden());
    }

    @Test
    void meuPerfil_quandoCadastroNaoExiste_retorna404() throws Exception {
        doThrow(new RecursoNaoEncontradoException(
                "Perfil do chefe do FUSEX nao encontrado"))
                .when(chefePerfilService)
                .buscarPerfilLogado();

        mockMvc.perform(get("/chefe/me")
                        .header(
                                SimulacaoLoginFilter.HEADER,
                                "chefe-teste"))
                .andExpect(status().isNotFound());
    }

    @Test
    void meuPerfil_quandoNaoAutenticado_retorna401() throws Exception {
        doThrow(new NaoAutenticadoException("Usuario nao autenticado"))
                .when(chefePerfilService)
                .buscarPerfilLogado();

        mockMvc.perform(get("/chefe/me"))
                .andExpect(status().isUnauthorized());
    }
}